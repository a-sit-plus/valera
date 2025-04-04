package at.asitplus.wallet.app.common

import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.ktor.openid.OpenId4VciClient
import at.asitplus.wallet.lib.ktor.openid.ProvisioningContext
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.oidvci.buildClientAttestationJwt
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class ProvisioningService(
    val platformAdapter: PlatformAdapter,
    private val dataStoreService: DataStoreService,
    private val cryptoService: CryptoService,
    holderAgent: HolderAgent,
    private val config: WalletConfig,
    errorService: ErrorService,
    httpService: HttpService,
) {
    /** Checked by appLink handling whether to jump into [resumeWithAuthCode] */
    var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/provisioning"
    private val clientId = "https://wallet.a-sit.at/app"
    private val clientAttestationJwt = runBlocking {
        DefaultJwsService(cryptoService).buildClientAttestationJwt(
            clientId = clientId,
            issuer = "https://example.com",
            lifetime = 60.minutes,
            clientKey = cryptoService.keyMaterial.jsonWebKey
        ).serialize()
    }

    private val openId4VciClient = OpenId4VciClient(
        openUrlExternally = {
            this.redirectUri = redirectUrl
            platformAdapter.openUrl(it)
        },
        engine = HttpClient().engine,
        cookiesStorage = cookieStorage,
        httpClientConfig = httpService.loggingConfig,
        storeProvisioningContext = {
            dataStoreService.setPreference(
                key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
                value = vckJsonSerializer.encodeToString(it),
            )
        },
        loadProvisioningContext = {
            dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT).firstOrNull()
                ?.let {
                    vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                        .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
                }
        },
        loadClientAttestationJwt = { clientAttestationJwt },
        clientAttestationJwsService = DefaultJwsService(cryptoService),
        oid4vciService = WalletService(clientId, redirectUrl, cryptoService),
        storeCredential = { cred ->
            runCatching { holderAgent.storeCredential(cred) }.onFailure { Napier.w("Could not store $cred", it)}
        },
        storeRefreshToken = {} // TODO store refresh tokens to refresh credentials later on
    )

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(host: String) = openId4VciClient.loadCredentialMetadata(host).getOrThrow()

    /**
     * Starts the issuing process at [credentialIssuer]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioningWithAuthRequest(
        credentialIssuer: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
    ) {
        config.set(host = credentialIssuer)
        cookieStorage.reset()
        openId4VciClient.startProvisioningWithAuthRequest(
            credentialIssuer,
            credentialIdentifierInfo,
        ).getOrThrow()
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        this.redirectUri = null
        openId4VciClient.resumeWithAuthCode(redirectedUrl).getOrThrow()
    }

    /**
     * Decodes the content of a scanned QR code, expected to contain a [at.asitplus.openid.CredentialOffer].
     *
     * @param qrCodeContent as scanned
     */
    @Throws(Throwable::class)
    suspend fun decodeCredentialOffer(
        qrCodeContent: String
    ): CredentialOffer {
        val walletService = WalletService(
            cryptoService = cryptoService,
            remoteResourceRetriever = { data ->
                withContext(Dispatchers.IO) {
                    client.get(data.url).bodyAsText()
                }
            })
        return walletService.parseCredentialOffer(qrCodeContent).getOrThrow()
    }

    /**
     * Loads a user-selected credential with pre-authorized code from the OID4VCI credential issuer
     *
     * @param credentialOffer as loaded and decoded from the QR Code
     * @param credentialIdentifierInfo as selected by the user from the issuer's metadata
     * @param transactionCode if required from Issuing service, i.e. transmitted out-of-band to the user
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithOffer(
        credentialOffer: CredentialOffer,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        transactionCode: String? = null
    ) {
        openId4VciClient.loadCredentialWithOffer(
            credentialOffer,
            credentialIdentifierInfo,
            transactionCode
        ).getOrThrow()
    }
}

val CredentialIdentifierInfo.credentialScheme: ConstantIndex.CredentialScheme?
    get() = with(supportedCredentialFormat) {
        (credentialDefinition?.types?.firstNotNullOfOrNull { AttributeIndex.resolveAttributeType(it) }
            ?: sdJwtVcType?.let { AttributeIndex.resolveSdJwtAttributeType(it) }
            ?: docType?.let { AttributeIndex.resolveIsoDoctype(it) })
    }