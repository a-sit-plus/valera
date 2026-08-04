package at.asitplus.wallet.app.common

import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.OAuth2AuthorizationServerMetadata
import at.asitplus.openid.SupportedCredentialFormat
import at.asitplus.openid.SupportedCredentialFormatIsoMdoc
import at.asitplus.openid.SupportedCredentialFormatSdJwt
import at.asitplus.openid.SupportedCredentialFormatW3cVcJsonLd
import at.asitplus.openid.SupportedCredentialFormatW3cVcJwt
import at.asitplus.openid.SupportedCredentialFormatW3cVcJwtJsonLd
import at.asitplus.signum.indispensable.equalsCryptographically
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.josef.toJsonWebKey
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import at.asitplus.wallet.lib.ktor.openid.OAuth2KtorClient
import at.asitplus.wallet.lib.ktor.openid.OpenId4VciClient
import at.asitplus.wallet.lib.ktor.openid.ProvisioningContext
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.WalletService
import at.asitplus.wallet.lib.utils.MapStore
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import data.storage.persistentStringMapStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ui.navigation.IntentService
import kotlin.time.Duration.Companion.milliseconds


class ProvisioningService(
    private val intentService: IntentService,
    private val dataStoreService: DataStoreService,
    private val keyMaterial: KeyMaterial,
    private val keystoreService: KeystoreService,
    private val holderAgent: HolderAgent,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val config: SettingsRepository,
    private val errorService: ErrorService,
    private val httpService: HttpService,
    private val attestationService: AttestationService
) {
    data class StoredCredentialIssuanceResult(
        val credentialIssuanceResult: CredentialIssuanceResult,
        val storedEntryIds: List<StoreEntryId> = emptyList(),
    )

    private val cookieStorage = PersistentCookieStorage(dataStoreService, errorService)
    private val client = httpService.buildHttpClient(cookieStorage = cookieStorage)
    private val stateToCodeStore = persistentStringMapStore(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_PROVISIONING_STATE_TO_CODE_STORE,
    )
    private val provisioningContextStore = persistentStringMapStore(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT_BY_STATE,
    )
    private val provisioningInstanceAttestationStore = persistentStringMapStore(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE,
    )
    private val activeProvisioningStateStore = persistentStringMapStore(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_ACTIVE_PROVISIONING_STATE,
    )

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.plus/app/callback/provisioning"
    private var cachedClientId: String? = null
    private var cachedWalletProviderAttestationEnabled: Boolean? = null

    private var openId4VciClientCached = null as OpenId4VciClient?
    private var walletServiceCached = null as WalletService?
    private val provisioningFlowMutex = Mutex()

    private suspend fun currentClientId(): String {
        val clientId = config.clientId.first()
        if (clientId != cachedClientId) {
            cachedClientId = clientId
            clearCaches()
        }
        return clientId
    }

    private suspend fun currentWalletProviderAttestationEnabled(): Boolean {
        val enabled = config.walletProviderAttestationEnabled.first()
        if (enabled != cachedWalletProviderAttestationEnabled) {
            cachedWalletProviderAttestationEnabled = enabled
            clearCaches()
        }
        return enabled
    }

    private suspend fun clearCaches(resetAttestation: Boolean = true) {
        if (resetAttestation && activeProvisioningStateStore.get(ACTIVE_PROVISIONING_STATE_KEY) == null) {
            attestationService.reset()
        } else if (resetAttestation) {
            Napier.w("Keeping instance attestation while a provisioning browser flow is active")
        }
        openId4VciClientCached = null
        walletServiceCached = null
    }

    private suspend fun walletService(): WalletService = walletServiceCached ?: run {
        val clientId = currentClientId()
        currentWalletProviderAttestationEnabled()
        WalletService(
            clientId = clientId,
            loadKeyAttestation = attestationService::loadKeyAttestation,
            keyMaterial = keyMaterial,
            remoteResourceRetriever = { data ->
                withContext(Dispatchers.IO) {
                    client.get(data.url).bodyAsText()
                }
            }
        )
    }.also { walletServiceCached = it }

    private suspend fun openId4VciClient(): OpenId4VciClient {
        assertProofKeyConsistent()
        return openId4VciClientCached ?: buildOpenId4VciClient()
    }

    /**
     * Verifies that the [keyMaterial] used to sign OID4VCI credential-request proofs still matches the
     * key actually present in the platform key store. A mismatch means the in-memory key material has
     * gone stale (e.g. the hardware key was invalidated and re-created); signing a proof with it would
     * be rejected by the issuer at `ProofValidator.validateJwtProof`. We fail fast with a clear error
     * and drop all cached clients/attestations, so a fresh key (and a matching key attestation) is used
     * once the key material has been re-derived.
     */
    private suspend fun assertProofKeyConsistent() {
        val proofThumbprint = keyMaterial.jsonWebKey.jwkThumbprint
        val liveKey = keystoreService.liveSigningPublicKey()
        val liveThumbprint = liveKey?.toJsonWebKey()?.jwkThumbprint
        Napier.d("Proof key consistency check: proofKey=$proofThumbprint, liveKeyStore=$liveThumbprint")
        if (liveKey == null) {
            Napier.w("Proof key consistency check: no live signing key under KS_ALIAS, skipping guard")
            return
        }
        if (!liveKey.equalsCryptographically(keyMaterial.publicKey)) {
            Napier.e(
                "Proof signing key mismatch: proof would be signed with $proofThumbprint, " +
                        "but key store holds $liveThumbprint. Invalidating caches."
            )
            clearCaches()
            throw ProofKeyMismatchException(expected = liveThumbprint, actual = proofThumbprint)
        }
    }

    private suspend fun buildOpenId4VciClient(): OpenId4VciClient = run {
        val clientId = currentClientId()
        val walletProviderAttestationEnabled = currentWalletProviderAttestationEnabled()
        val oAuth2Client = OAuth2Client(
            clientId = clientId,
            redirectUrl = redirectUrl,
            stateToCodeStore = stateToCodeStore
        )
        val oAuth2KtorClient = if (walletProviderAttestationEnabled) {
            OAuth2KtorClient(
                engine = client.engine,
                cookiesStorage = cookieStorage,
                oAuth2Client = oAuth2Client,
                httpClientConfig = httpService.loggingConfig,
                loadInstanceAttestation = attestationService::loadInstanceAttestation,
                keyMaterial = attestationService.getInstanceAttestationKeyMaterial()
            )
        } else {
            OAuth2KtorClient(
                engine = client.engine,
                cookiesStorage = cookieStorage,
                oAuth2Client = oAuth2Client,
                httpClientConfig = httpService.loggingConfig,
            )
        }
        OpenId4VciClient(
            engine = client.engine,
            cookiesStorage = cookieStorage,
            httpClientConfig = httpService.loggingConfig,
            oauth2Client = oAuth2KtorClient,
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
     * Parses issuer metadata into credential identifier info.
     */
    @Throws(Throwable::class)
    suspend fun parseCredentialMetadata(issuerMetadata: IssuerMetadata) =
        openId4VciClient().parseCredentialMetadata(issuerMetadata).getOrThrow()

    /**
     * Starts the issuing process at [credentialIssuer]
     */
    @Throws(Throwable::class)
    suspend fun startProvisioningWithAuthRequest(
        credentialIssuer: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        reissuingStoreEntryId: StoreEntryId? = null
    ) {
        provisioningFlowMutex.withLock {
            clearAbandonedProvisioningFlow()
            clearCaches()
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
    }

    private suspend fun CredentialIssuanceResult.OpenUrlForAuthnRequest.storeContextOpenIntent() {
        provisioningContextStore.put(context.state, joseCompliantSerializer.encodeToString(context))
        storeCurrentInstanceAttestation(context.state)
        markActiveProvisioningState(context.state)
        Napier.d("Stored provisioning context for state ${context.state}")
        try {
            intentService.openIntent(
                url = url,
                redirectUri = redirectUrl,
                intentType = IntentService.IntentType.ProvisioningResumeIntent
            )
        } catch (e: Throwable) {
            cleanupProvisioningState(context.state)
            throw e
        }
    }


    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(
        redirectedUrl: String,
        statusUpdater: ((Long, RefreshStatus) -> Unit)? = null,
        onProgress: ((LoadingMessageKey) -> Unit)? = null,
    ): List<StoreEntryId> {
        Napier.d("handleResponse with $redirectedUrl")
        val storedState = provisioningFlowMutex.withLock {
            takeProvisioningState(redirectedUrl)
        }
            ?: throw IllegalStateException(
                "No provisioning context found for callback state ${redirectedUrl.stateParameter()}"
            )

        try {
            storedState.instanceAttestation?.let {
                attestationService.restoreInstanceAttestation(it)
            }
            onProgress?.invoke(LoadingMessageKey.IssuingCredential)
            clearCaches(resetAttestation = false)
            return openId4VciClient().resumeWithAuthCode(redirectedUrl, storedState.context).getOrThrow().let { result ->
                val idsBefore =
                    subjectCredentialStore.observeStoreContainer().first().credentials.map { it.first }.toSet()
                Napier.d("resumeWithAuthCode idsBefore=$idsBefore")
                Napier.d("resumeWithAuthCode received ${result.credentials.size} credential(s)")
                onProgress?.invoke(LoadingMessageKey.StoringCredential)
                val storageResults = result.credentials.map { cred ->
                    holderAgent.storeCredential(cred, result.refreshToken)
                }
                val storedEntryIds = resolveNewStoreEntryIds(idsBefore)
                Napier.d("resumeWithAuthCode resolved storeEntryIds=$storedEntryIds")

                if (storageResults.all { it.isSuccess }) {
                    storedState.context.reissuingStoreEntryId?.let { id ->
                        subjectCredentialStore.removeStoreEntryById(id)
                        statusUpdater?.invoke(id, RefreshStatus.Succeeded)
                    }
                } else {
                    storedState.context.reissuingStoreEntryId?.let { statusUpdater?.invoke(it, RefreshStatus.Failed) }
                    storageResults.first { it.isFailure }.getOrThrow()
                }
                storedEntryIds
            }
        } finally {
            clearActiveProvisioningState(storedState.state)
        }
    }

    private data class StoredProvisioningState(
        val state: String,
        val context: ProvisioningContext,
        val instanceAttestation: JwsCompactTyped<JsonWebToken>?,
    )

    private suspend fun takeProvisioningState(redirectedUrl: String): StoredProvisioningState? {
        val state = redirectedUrl.stateParameter()
        state?.let { callbackState ->
            validateActiveProvisioningState(callbackState)
            provisioningContextStore.remove(callbackState)?.let { contextJson ->
                val instanceAttestation = provisioningInstanceAttestationStore.remove(callbackState)
                    ?.let { JwsCompactTyped<JsonWebToken>(it) }
                return StoredProvisioningState(
                    state = callbackState,
                    context = joseCompliantSerializer.decodeFromString<ProvisioningContext>(contextJson),
                    instanceAttestation = instanceAttestation,
                )
            }
        } ?: Napier.w("Provisioning callback has no state parameter")

        return null
    }

    private fun String.stateParameter(): String? = URLBuilder(this).parameters["state"]

    private suspend fun storeCurrentInstanceAttestation(state: String) {
        if (!config.walletProviderAttestationEnabled.first()) {
            return
        }
        val instanceAttestation = attestationService.currentInstanceAttestation()
        if (instanceAttestation == null) {
            Napier.w("No instance attestation cached for provisioning state $state")
            return
        }
        provisioningInstanceAttestationStore.put(state, instanceAttestation.toString())
    }

    /**
     * A leftover active state means the previous browser flow was abandoned (the user never
     * returned via the redirect): clear it and its stored context, so a new flow can start.
     */
    private suspend fun clearAbandonedProvisioningFlow() = clearAbandonedProvisioningFlow(
        activeProvisioningStateStore = activeProvisioningStateStore,
        provisioningContextStore = provisioningContextStore,
        provisioningInstanceAttestationStore = provisioningInstanceAttestationStore,
    )

    private suspend fun markActiveProvisioningState(state: String) {
        activeProvisioningStateStore.put(ACTIVE_PROVISIONING_STATE_KEY, state)
    }

    private suspend fun validateActiveProvisioningState(callbackState: String) {
        activeProvisioningStateStore.get(ACTIVE_PROVISIONING_STATE_KEY)?.let { activeState ->
            validateProvisioningCallbackStateValue(callbackState, activeState)
        }
    }

    private suspend fun clearActiveProvisioningState(state: String) {
        val activeState = activeProvisioningStateStore.get(ACTIVE_PROVISIONING_STATE_KEY)
        if (activeState == state) {
            activeProvisioningStateStore.remove(ACTIVE_PROVISIONING_STATE_KEY)
        }
    }

    private suspend fun cleanupProvisioningState(state: String) {
        provisioningContextStore.remove(state)
        provisioningInstanceAttestationStore.remove(state)
        clearActiveProvisioningState(state)
    }

    @Throws(Throwable::class)
    suspend fun refreshCredential(
        renewalInfo: CredentialRenewalInfo,
        oldCredentialId: StoreEntryId,
        statusUpdater: ((Long, RefreshStatus) -> Unit)
    ) {
        clearCaches()
        Napier.d("refreshCredential with identifier ${renewalInfo.credentialIdentifier}")
        openId4VciClient().refreshCredentialReturningResult(renewalInfo).getOrThrow().also { result ->
            val storageResults = result.credentials.map { credentialInput ->
                holderAgent.storeCredential(credentialInput, result.refreshToken)
            }

            if (storageResults.all { it.isSuccess }) {
                subjectCredentialStore.removeStoreEntryById(oldCredentialId)
                statusUpdater(oldCredentialId, RefreshStatus.Succeeded)
            } else {
                statusUpdater(oldCredentialId, RefreshStatus.Failed)
                storageResults.first { it.isFailure }.getOrThrow()
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
        clearCaches()
        return walletService().parseCredentialOffer(qrCodeContent).getOrThrow()
    }

    /**
     * Loads a user-selected credential with pre-authorized code from the OID4VCI credential issuer
     *
     * @param credentialOffer as loaded and decoded from the QR Code
     * @param credentialIdentifierInfo as selected by the user from the issuer's metadata
     * @param transactionCode if required from Issuing service, i.e. transmitted out-of-band to the user
     * @param authorizationServerMetadata oauthMetadata optionally transmitted via the DC API. Set to null for other flows.
     *
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithOffer(
        credentialOffer: CredentialOffer,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        transactionCode: String? = null,
        authorizationServerMetadata: OAuth2AuthorizationServerMetadata? = null,
        onProgress: ((LoadingMessageKey) -> Unit)? = null,
    ): StoredCredentialIssuanceResult {
        val result = provisioningFlowMutex.withLock {
            clearAbandonedProvisioningFlow()
            onProgress?.invoke(LoadingMessageKey.IssuingCredential)
            val result = openId4VciClient().loadCredentialWithOfferReturningResult(
                credentialOffer,
                credentialIdentifierInfo,
                transactionCode,
                authorizationServerMetadata
            ).getOrThrow()
            if (result is CredentialIssuanceResult.OpenUrlForAuthnRequest) {
                Napier.d("loadCredentialWithOffer requires browser auth")
                result.storeContextOpenIntent()
            }
            result
        }

        return when (result) {
            is CredentialIssuanceResult.OpenUrlForAuthnRequest ->
                StoredCredentialIssuanceResult(credentialIssuanceResult = result)

            is CredentialIssuanceResult.Success -> {
                val idsBefore =
                    subjectCredentialStore.observeStoreContainer().first().credentials.map { it.first }.toSet()
                Napier.d("loadCredentialWithOffer idsBefore=$idsBefore")
                Napier.d("loadCredentialWithOffer received ${result.credentials.size} credential(s) without browser auth")
                onProgress?.invoke(LoadingMessageKey.StoringCredential)
                result.credentials.forEach {
                    holderAgent.storeCredential(it).getOrThrow()
                }
                val storedEntryIds = resolveNewStoreEntryIds(idsBefore)
                Napier.d("loadCredentialWithOffer resolved storeEntryIds=$storedEntryIds")
                StoredCredentialIssuanceResult(
                    credentialIssuanceResult = result,
                    storedEntryIds = storedEntryIds,
                )
            }
        }
    }

    // Wait for the DataStore flow to emit a container that includes at least one new entry.
    private suspend fun resolveNewStoreEntryIds(idsBefore: Set<StoreEntryId>): List<StoreEntryId> {
        return withTimeoutOrNull(3_000.milliseconds) {
            subjectCredentialStore.observeStoreContainer()
                .map { container -> (container.credentials.map { it.first }.toSet() - idsBefore).toList() }
                .first { it.isNotEmpty() }
        } ?: run {
            Napier.w("resolveNewStoreEntryIds timed out waiting for new store entries")
            emptyList()
        }
    }

}

private fun validateProvisioningCallbackStateValue(callbackState: String, expectedState: String) {
    require(callbackState.isNotBlank()) { "Missing state in provisioning callback" }
    require(callbackState == expectedState) { "Provisioning callback state does not match active flow" }
}

internal fun validateProvisioningCallbackState(redirectedUrl: String, expectedState: String) {
    val callbackState = try {
        URLBuilder(redirectedUrl).parameters["state"]
    } catch (e: Throwable) {
        throw IllegalArgumentException("Invalid provisioning callback URL", e)
    }
    validateProvisioningCallbackStateValue(callbackState.orEmpty(), expectedState)
}

/** Removes an abandoned browser flow's active-state marker and per-state context, if any. */
internal suspend fun clearAbandonedProvisioningFlow(
    activeProvisioningStateStore: MapStore<String, String>,
    provisioningContextStore: MapStore<String, String>,
    provisioningInstanceAttestationStore: MapStore<String, String>,
) {
    activeProvisioningStateStore.remove(ACTIVE_PROVISIONING_STATE_KEY)?.let { state ->
        Napier.w("Clearing abandoned provisioning browser flow for state $state")
        provisioningContextStore.remove(state)
        provisioningInstanceAttestationStore.remove(state)
    }
}

internal const val ACTIVE_PROVISIONING_STATE_KEY = "active"
class ProofKeyMismatchException(expected: String?, actual: String?) : IllegalStateException(
    "The credential proof signing key no longer matches the device key store " +
            "(key store holds $expected, key material advertises $actual). " +
            "The key material must be re-derived before issuing credentials."
)

suspend fun SupportedCredentialFormat.resolveCredentialScheme(): CredentialScheme? = when (this) {
    is SupportedCredentialFormatIsoMdoc -> AttributeIndex.resolveIdentifier(docType, ISO_MDOC)
    is SupportedCredentialFormatSdJwt -> AttributeIndex.resolveIdentifier(sdJwtVcType, SD_JWT)
    is SupportedCredentialFormatW3cVcJwt -> AttributeIndex.resolveIdentifierPlainJwt(credentialDefinition.types)
    is SupportedCredentialFormatW3cVcJsonLd -> AttributeIndex.resolveIdentifierPlainJwt(credentialDefinition.type)
    is SupportedCredentialFormatW3cVcJwtJsonLd -> AttributeIndex.resolveIdentifierPlainJwt(credentialDefinition.type)
}

