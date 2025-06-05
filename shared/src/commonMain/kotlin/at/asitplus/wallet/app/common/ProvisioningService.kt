package at.asitplus.wallet.app.common

import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.JwsHeaderCertOrJwk
import at.asitplus.wallet.lib.jws.JwsHeaderNone
import at.asitplus.wallet.lib.jws.SignJwt
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import at.asitplus.wallet.lib.ktor.openid.OpenId4VciClient
import at.asitplus.wallet.lib.ktor.openid.ProvisioningContext
import at.asitplus.wallet.lib.oidvci.BuildClientAttestationJwt
import at.asitplus.wallet.lib.oidvci.WalletService
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import ui.navigation.IntentService
import kotlin.time.Duration.Companion.minutes


class ProvisioningService(
    private val intentService: IntentService,
    private val dataStoreService: DataStoreService,
    private val keyMaterial: KeyMaterial,
    private val holderAgent: HolderAgent,
    private val config: SettingsRepository,
    private val errorService: ErrorService,
    httpService: HttpService,
) {
    /** Checked by appLink handling whether to jump into [resumeWithAuthCode] */
    private var redirectUri: String? = null
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/provisioning"
    private val clientId = "https://wallet.a-sit.at/app"

    private var clientAttestationJwt = null as String?
    suspend fun clientAttestationJwt() = clientAttestationJwt ?: BuildClientAttestationJwt(
        SignJwt(keyMaterial, JwsHeaderCertOrJwk()),
        clientId = clientId,
        issuer = "https://example.com",
        lifetime = 60.minutes,
        clientKey = keyMaterial.jsonWebKey
    ).serialize().also {
        clientAttestationJwt = it
    }

    private val openId4VciClient = OpenId4VciClient(
        engine = HttpClient().engine,
        cookiesStorage = cookieStorage,
        httpClientConfig = httpService.loggingConfig,
        loadClientAttestationJwt = { clientAttestationJwt() },
        signClientAttestationPop = SignJwt(keyMaterial, JwsHeaderNone()),
        oid4vciService = WalletService(clientId, redirectUrl, keyMaterial),
    )

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(host: String) =
        openId4VciClient.loadCredentialMetadata(host).getOrThrow()

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
        openId4VciClient.startProvisioningWithAuthRequestReturningResult(
            credentialIssuer,
            credentialIdentifierInfo
        ).getOrThrow().run {
            storeContextOpenIntent()
        }
    }

    private suspend fun CredentialIssuanceResult.OpenUrlForAuthnRequest.storeContextOpenIntent() {
        dataStoreService.setPreference(
            key = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT,
            value = vckJsonSerializer.encodeToString(context),
        )
        intentService.openIntent(
            url = url,
            redirectUri = redirectUrl,
            intentType = IntentService.IntentType.ProvisioningIntent
        )
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(redirectedUrl: String) {
        Napier.d("handleResponse with $redirectedUrl")
        this.redirectUri = null
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
            .firstOrNull()
            ?.let {
                vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                    .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
            }?.let {
                openId4VciClient.resumeWithAuthCode(redirectedUrl, it).getOrThrow()
                    .credentials.forEach {
                        holderAgent.storeCredential(it).onFailure { ex ->
                            Napier.e("storeCredential failed", ex)
                        }
                    }
            }
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
            keyMaterial = keyMaterial,
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
        openId4VciClient.loadCredentialWithOfferReturningResult(
            credentialOffer,
            credentialIdentifierInfo,
            transactionCode
        ).getOrThrow().run {
            when (this) {
                is CredentialIssuanceResult.OpenUrlForAuthnRequest -> storeContextOpenIntent()
                is CredentialIssuanceResult.Success -> {
                    credentials.forEach {
                        holderAgent.storeCredential(it).onFailure { ex ->
                            Napier.e("storeCredential failed", ex)
                        }
                    }
                }
            }
        }
    }
}

val CredentialIdentifierInfo.credentialScheme: ConstantIndex.CredentialScheme?
    get() = with(supportedCredentialFormat) {
        (credentialDefinition?.types?.firstNotNullOfOrNull { AttributeIndex.resolveAttributeType(it) }
            ?: sdJwtVcType?.let { AttributeIndex.resolveSdJwtAttributeType(it) }
            ?: docType?.let { AttributeIndex.resolveIsoDoctype(it) })
    }
