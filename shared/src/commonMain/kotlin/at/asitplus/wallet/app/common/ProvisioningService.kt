package at.asitplus.wallet.app.common

import at.asitplus.catching
import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import at.asitplus.wallet.lib.ktor.openid.OAuth2KtorClient
import at.asitplus.wallet.lib.ktor.openid.OpenId4VciClient
import at.asitplus.wallet.lib.ktor.openid.ProvisioningContext
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.WalletService
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import ui.navigation.IntentService


class ProvisioningService(
    private val intentService: IntentService,
    private val dataStoreService: DataStoreService,
    private val keyMaterial: KeyMaterial,
    private val holderAgent: HolderAgent,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val config: SettingsRepository,
    private val errorService: ErrorService,
    private val httpService: HttpService,
    private val attestationService: AttestationService,
) {
    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.at/app/callback/provisioning"
    private var cachedClientId: String? = null

    private var openId4VciClientCached = null as OpenId4VciClient?
    private var walletServiceCached = null as WalletService?

    private suspend fun currentClientId(): String {
        val clientId = config.clientId.first()
        if (clientId != cachedClientId) {
            cachedClientId = clientId
            openId4VciClientCached = null
            walletServiceCached = null
        }
        return clientId
    }

    private suspend fun walletService(): WalletService = walletServiceCached ?: run {
        val clientId = currentClientId()
        WalletService(
            clientId = clientId,
            keyMaterial = keyMaterial,
            loadKeyAttestation = attestationService::loadKeyAttestation,
            remoteResourceRetriever = { data ->
                withContext(Dispatchers.IO) {
                    client.get(data.url).bodyAsText()
                }
            }
        )
    }.also { walletServiceCached = it }

    private suspend fun openId4VciClient(): OpenId4VciClient = openId4VciClientCached ?: run {
        OpenId4VciClient(
            engine = client.engine,
            cookiesStorage = cookieStorage,
            httpClientConfig = httpService.loggingConfig,
            oauth2Client = OAuth2KtorClient(
                engine = client.engine,
                cookiesStorage = cookieStorage,
                oAuth2Client = OAuth2Client(clientId = currentClientId(), redirectUrl = redirectUrl),
                httpClientConfig = httpService.loggingConfig,
                loadInstanceAttestation = attestationService::loadInstanceAttestation,
                loadInstanceAttestationPop = { attestationService.loadInstanceAttestationPop() },
            ),
            oid4vciService = walletService()
        ).also { openId4VciClientCached = it }
    }

    /**
     * Loads credential metadata info from [host]
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(host: String) =
        openId4VciClient().loadCredentialMetadata(host).getOrThrow()

    /**
     * Starts the issuing process at [credentialIssuer]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioningWithAuthRequest(
        credentialIssuer: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        reissuingStoreEntryId: StoreEntryId? = null
    ) {
        config.set(host = credentialIssuer)
        cookieStorage.reset()
        openId4VciClient().startProvisioningWithAuthRequestReturningResult(
            credentialIssuer,
            credentialIdentifierInfo,
            reissuingStoreEntryId
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
            intentType = IntentService.IntentType.ProvisioningResumeIntent
        )
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(redirectedUrl: String, statusUpdater: ((Long, RefreshStatus) -> Unit)? = null) {
        Napier.d("handleResponse with $redirectedUrl")
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
            .firstOrNull()
            ?.let {
                vckJsonSerializer.decodeFromString<ProvisioningContext>(it)
                    .also { dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT) }
            }?.let { context ->
                openId4VciClient().resumeWithAuthCode(redirectedUrl, context).getOrThrow().also { result ->
                    val storageResults = result.credentials.map { cred ->
                        holderAgent.storeCredential(cred, result.refreshToken)
                    }

                    if (storageResults.all { it.isSuccess }) {
                        context.reissuingStoreEntryId?.let { id ->
                            subjectCredentialStore.removeStoreEntryById(id)
                            statusUpdater?.invoke(id, RefreshStatus.Succeeded)
                        }
                    } else {
                        storageResults.filter { it.isFailure }.forEach {
                            Napier.e("storeCredential failed", it.exceptionOrNull())
                        }
                        context.reissuingStoreEntryId?.let { statusUpdater?.invoke(it, RefreshStatus.Failed) }
                    }
                }
            }
    }

    @Throws(Throwable::class)
    suspend fun refreshCredential(renewalInfo: CredentialRenewalInfo, oldCredentialId: StoreEntryId, statusUpdater: ((Long, RefreshStatus) -> Unit)) {
        Napier.d("refreshCredential with identifier ${renewalInfo.credentialIdentifier}")
        openId4VciClient().refreshCredentialReturningResult(renewalInfo).getOrThrow().also { result ->
            val storageResults = result.credentials.map { credentialInput ->
                holderAgent.storeCredential(credentialInput, result.refreshToken)
            }

            if (storageResults.all { it.isSuccess }) {
                subjectCredentialStore.removeStoreEntryById(oldCredentialId)
                statusUpdater(oldCredentialId, RefreshStatus.Succeeded)
            } else {
                storageResults.filter { it.isFailure }.forEach {
                    Napier.e("storeCredential failed", it.exceptionOrNull())
                }
                statusUpdater(oldCredentialId, RefreshStatus.Failed)
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
        return walletService().parseCredentialOffer(qrCodeContent).getOrThrow()
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
        openId4VciClient().loadCredentialWithOfferReturningResult(
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
