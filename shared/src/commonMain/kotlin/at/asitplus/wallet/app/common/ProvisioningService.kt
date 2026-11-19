package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.data.NonEmptyList
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
import at.asitplus.wallet.app.common.validation.IssuerValidationUi
import at.asitplus.wallet.app.common.validation.PendingCredentialReview
import at.asitplus.wallet.app.common.validation.ValidationPolicies
import at.asitplus.wallet.app.common.validation.describeProblems
import at.asitplus.wallet.app.common.validation.integrityRejection
import at.asitplus.wallet.app.common.validation.needsReview
import at.asitplus.wallet.app.common.validation.rejection
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.PreparedCredentialStorage
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.ktor.openid.CredentialIssuanceResult
import at.asitplus.wallet.lib.ktor.openid.IssuedCredentials
import at.asitplus.wallet.lib.ktor.openid.OpenId4VciKtorClient
import at.asitplus.wallet.lib.ktor.openid.ProvisioningContext
import at.asitplus.wallet.lib.oauth2.ClientAttestation
import at.asitplus.wallet.lib.oauth2.OAuth2Client
import at.asitplus.wallet.lib.oidvci.AttestationProofCreator
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
import at.asitplus.wallet.lib.oidvci.JwtProofCreator
import at.asitplus.wallet.lib.oidvci.LoadedIssuerMetadata
import at.asitplus.wallet.lib.oidvci.OpenId4VciClient
import at.asitplus.wallet.lib.oidvci.ReceivedCredential
import at.asitplus.wallet.lib.validation.IssuerMetadataValidator
import at.asitplus.wallet.lib.validation.ValidationDecision
import data.credentials.claimLabel
import data.storage.DataStoreService
import data.storage.PersistentCookieStorage
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import data.storage.persistentStringMapStore
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ui.navigation.IntentService


class ProvisioningService(
    private val intentService: IntentService,
    private val dataStoreService: DataStoreService,
    private val keyMaterial: KeyMaterial,
    private val keystoreService: KeystoreService,
    private val holderAgent: HolderAgent,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
    private val config: SettingsRepository,
    private val errorService: ErrorService,
    httpService: HttpService,
    private val attestationService: AttestationService,
    private val issuerMetadataValidator: IssuerMetadataValidator,
    private val pendingCredentialReview: PendingCredentialReview,
) {
    /** The [credentialIdentifiers] a credential issuer offers, and the [issuer] as validated from its metadata. */
    data class IssuerMetadataLoad(
        val credentialIdentifiers: Collection<CredentialIdentifierInfo>,
        val issuer: IssuerValidationUi,
    )

    data class StoredCredentialIssuanceResult(
        val credentialIssuanceResult: CredentialIssuanceResult,
        val storedEntryIds: List<StoreEntryId> = emptyList(),
        /** Whether credentials wait for the user to decide whether to store them, see [credentialsUnderReview]. */
        val needsReview: Boolean = false,
    )

    /** The credentials that have been stored, and whether others wait for review, see [credentialsUnderReview]. */
    data class StoredCredentials(
        val storedEntryIds: List<StoreEntryId>,
        val needsReview: Boolean,
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
    private val browserProvisioning = BrowserProvisioningFlow(
        activeStates = persistentStringMapStore(
            dataStoreService, Configuration.DATASTORE_KEY_ACTIVE_PROVISIONING_STATE,
        ),
        contexts = provisioningContextStore,
        attestations = provisioningInstanceAttestationStore,
        codeVerifiers = stateToCodeStore,
    )

    private val redirectUrl = "asitplus-wallet://wallet.a-sit.plus/app/callback/provisioning"
    private var cachedClientId: String? = null
    private var cachedWalletProviderAttestationEnabled: Boolean? = null

    private var openId4VciClientCached = null as OpenId4VciKtorClient?
    private var vciClientCached = null as OpenId4VciClient?
    private val provisioningFlowMutex = Mutex()

    fun close() {
        client.close()
        // HttpService supplies a dedicated engine; cancelling it also stops VC-K's client copies.
        client.engine.close()
        client.engine.cancel()
    }

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

    private suspend fun clearCaches(resetAttestation: Boolean = true, startingBrowserFlow: Boolean = false) {
        if (resetAttestation && (startingBrowserFlow || !browserProvisioning.isActive())) {
            attestationService.reset()
        } else if (resetAttestation) {
            Napier.w("Keeping instance attestation while a provisioning browser flow is active")
        }
        openId4VciClientCached = null
        vciClientCached = null
    }

    private suspend fun vciClient(): OpenId4VciClient = vciClientCached ?: OpenId4VciClient(
        proofCreators = listOf(
            JwtProofCreator(keyMaterial = keyMaterial, loadKeyAttestation = attestationService::loadKeyAttestation),
            AttestationProofCreator(attestationService::loadKeyAttestation),
        ),
    ).also { vciClientCached = it }

    private suspend fun openId4VciClient(): OpenId4VciKtorClient {
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

    private suspend fun buildOpenId4VciClient(): OpenId4VciKtorClient = run {
        val clientId = currentClientId()
        val walletProviderAttestationEnabled = currentWalletProviderAttestationEnabled()
        val oAuth2Client = OAuth2Client(
            clientId = clientId,
            redirectUrl = redirectUrl,
            stateToCodeStore = stateToCodeStore
        )
        val clientAttestation = if (walletProviderAttestationEnabled) {
            ClientAttestation(
                keyMaterial = attestationService.getInstanceAttestationKeyMaterial(),
                loadInstanceAttestation = attestationService::loadInstanceAttestation,
            )
        } else null
        OpenId4VciKtorClient(
            httpClient = client,
            oauth2Client = oAuth2Client,
            vciClient = vciClient(),
            clientAttestation = clientAttestation,
            dpopKeyMaterial = clientAttestation?.keyMaterial ?: EphemeralKeyWithoutCert(),
            issuerMetadataValidator = issuerMetadataValidator,
        ).also { openId4VciClientCached = it }
    }

    /**
     * Loads the metadata of the credential issuer [host], preferring signed metadata, which are validated under
     * [ValidationPolicies.strict]. Metadata of an issuer that is not trusted are used nevertheless, so that the user
     * can decide after being warned, but metadata whose signature or subject failed are not.
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialMetadata(host: String): IssuerMetadataLoad {
        val client = openId4VciClient()
        val loaded = client.loadIssuerMetadata(host, ValidationPolicies.strict).getOrThrow()
        val issuer = IssuerValidationUi.of(loaded)
        if (issuer.outcome?.integrityFailed == true) {
            throw (loaded as? LoadedIssuerMetadata.Signed)?.validation?.report?.integrityRejection()
                ?: IllegalArgumentException("The metadata of $host have been rejected")
        }
        return IssuerMetadataLoad(client.parseCredentialMetadataIgnoringTrust(loaded).getOrThrow(), issuer)
    }

    /**
     * Parses unsigned issuer metadata into credential identifier info, e.g. those of an offer through the Digital
     * Credentials API.
     */
    @Throws(Throwable::class)
    suspend fun parseCredentialMetadata(issuerMetadata: IssuerMetadata) = IssuerMetadataLoad(
        credentialIdentifiers = openId4VciClient().parseCredentialMetadata(issuerMetadata).getOrThrow(),
        issuer = IssuerValidationUi.unsigned(),
    )

    /**
     * Starts the issuing process at [credentialIssuer] for all of [credentialIdentifierInfos] with one authorization
     */
    @Throws(Throwable::class)
    suspend fun startProvisioningWithAuthRequest(
        credentialIssuer: String,
        credentialIdentifierInfos: NonEmptyList<CredentialIdentifierInfo>,
        reissuingStoreEntryId: StoreEntryId? = null
    ) {
        provisioningFlowMutex.withLock {
            startBrowserProvisioning {
                clearCaches(startingBrowserFlow = true)
                config.set(host = credentialIssuer)
                cookieStorage.reset()
                openId4VciClient().startProvisioningWithAuthRequestReturningResult(
                    credentialIssuer,
                    credentialIdentifierInfos,
                    reissuingStoreEntryId,
                ).getOrThrow()
            }
        }
    }

    private suspend fun startBrowserProvisioning(
        prepare: suspend () -> CredentialIssuanceResult.OpenUrlForAuthnRequest,
    ) = browserProvisioning.start(
        prepare = prepare,
        stateOf = { it.context.state },
        persist = { request ->
            val state = request.context.state
            provisioningContextStore.put(state, joseCompliantSerializer.encodeToString(request.context))
            storeCurrentInstanceAttestation(state)
            Napier.d("Stored provisioning context for state $state")
        },
        open = { request ->
            intentService.openIntent(
                url = request.url,
                redirectUri = redirectUrl,
                intentType = IntentService.IntentType.ProvisioningResumeIntent,
            )
        },
    )

    /**
     * Called after getting the redirect back from ID Austria to the Issuing Service
     */
    @Throws(Throwable::class)
    suspend fun resumeWithAuthCode(
        redirectedUrl: String,
        statusUpdater: ((Long, RefreshStatus) -> Unit)? = null,
        onProgress: ((LoadingMessageKey) -> Unit)? = null,
    ): StoredCredentials {
        Napier.d("handleResponse with $redirectedUrl")
        val state = URLBuilder(redirectedUrl).parameters["state"]
        require(!state.isNullOrBlank()) { "Missing state in provisioning callback" }
        return browserProvisioning.resume(state) { contextJson, instanceAttestation ->
            val context = joseCompliantSerializer.decodeFromString<ProvisioningContext>(contextJson)
            val reissuingId = context.reissuingStoreEntryId
            try {
                if (config.walletProviderAttestationEnabled.first()) {
                    instanceAttestation?.let {
                        attestationService.restoreInstanceAttestation(JwsCompactTyped<JsonWebToken>(it))
                    }
                }
                onProgress?.invoke(LoadingMessageKey.IssuingCredential)
                clearCaches(resetAttestation = false)
                val result = openId4VciClient().resumeWithAuthCode(redirectedUrl, context).getOrThrow()
                val idsBefore =
                    subjectCredentialStore.observeStoreContainer().first().credentials.map { it.first }.toSet()
                Napier.d("resumeWithAuthCode received ${result.credentialsByConfiguration.size} credential(s)")
                onProgress?.invoke(LoadingMessageKey.StoringCredential)
                // A credential replacing a stored one is not reviewed, as the stored one is removed below.
                val underReview = mutableListOf<PendingCredentialReview.Entry>().takeIf { reissuingId == null }
                val storageResults = result.credentialsByConfiguration.map { (issued, credential) ->
                    storeCredential(credential, issued.scheme, issued.refreshToken, underReview)
                }
                val storedEntryIds = resolveNewStoreEntryIds(idsBefore, storageResults.count { it.getOrNull() != null })
                storageResults.firstOrNull { it.isFailure }?.getOrThrow()
                reissuingId?.let { id ->
                    subjectCredentialStore.removeStoreEntryById(id)
                    statusUpdater?.invoke(id, RefreshStatus.Succeeded)
                }
                underReview?.takeIf { it.isNotEmpty() }?.let { pendingCredentialReview.replace(it) }
                StoredCredentials(storedEntryIds, storageResults.any { it.getOrNull() == null })
            } catch (e: Throwable) {
                reissuingId?.let { statusUpdater?.invoke(it, RefreshStatus.Failed) }
                throw e
            }
        }
    }

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

    @Throws(Throwable::class)
    suspend fun refreshCredential(
        renewalInfo: CredentialRenewalInfo,
        oldCredentialId: StoreEntryId,
        statusUpdater: ((Long, RefreshStatus) -> Unit)
    ) {
        clearCaches()
        Napier.d("refreshCredential with identifier ${renewalInfo.credentialIdentifier}")
        openId4VciClient().refreshCredentialReturningResult(renewalInfo).getOrThrow().also { result ->
            val storageResults = result.credentialsByConfiguration.map { (issued, credential) ->
                storeCredential(credential, issued.scheme, issued.refreshToken, underReview = null)
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
        return openId4VciClient().loadCredentialOffer(qrCodeContent).getOrThrow()
    }

    /**
     * Loads the user-selected credentials with pre-authorized code from the OID4VCI credential issuer, with one token
     * for all of them
     *
     * @param credentialOffer as loaded and decoded from the QR Code
     * @param credentialIdentifierInfos as selected by the user from the issuer's metadata
     * @param transactionCode if required from Issuing service, i.e. transmitted out-of-band to the user
     * @param authorizationServerMetadata oauthMetadata optionally transmitted via the DC API. Set to null for other flows.
     *
     */
    @Throws(Throwable::class)
    suspend fun loadCredentialWithOffer(
        credentialOffer: CredentialOffer,
        credentialIdentifierInfos: NonEmptyList<CredentialIdentifierInfo>,
        transactionCode: String? = null,
        authorizationServerMetadata: OAuth2AuthorizationServerMetadata? = null,
        onProgress: ((LoadingMessageKey) -> Unit)? = null,
    ): StoredCredentialIssuanceResult {
        val result = provisioningFlowMutex.withLock {
            val request = suspend {
                onProgress?.invoke(LoadingMessageKey.IssuingCredential)
                openId4VciClient().loadCredentialWithOfferReturningResult(
                    credentialOffer,
                    credentialIdentifierInfos,
                    transactionCode,
                    authorizationServerMetadata,
                ).getOrThrow()
            }
            // VC-K prefers the pre-authorized grant when present. Only the authorization-code path needs the browser.
            if (credentialOffer.grants?.preAuthorizedCode != null) {
                request()
            } else {
                startBrowserProvisioning { request() as CredentialIssuanceResult.OpenUrlForAuthnRequest }
            }
        }

        return when (result) {
            is CredentialIssuanceResult.OpenUrlForAuthnRequest ->
                StoredCredentialIssuanceResult(credentialIssuanceResult = result)

            is CredentialIssuanceResult.Success -> {
                val idsBefore =
                    subjectCredentialStore.observeStoreContainer().first().credentials.map { it.first }.toSet()
                Napier.d("loadCredentialWithOffer idsBefore=$idsBefore")
                Napier.d("loadCredentialWithOffer received ${result.credentialsByConfiguration.size} credential(s) without browser auth")
                onProgress?.invoke(LoadingMessageKey.StoringCredential)
                val underReview = mutableListOf<PendingCredentialReview.Entry>()
                val storedEntries = result.credentialsByConfiguration.map { (issued, credential) ->
                    storeCredential(credential, issued.scheme, underReview = underReview).getOrThrow()
                }
                if (underReview.isNotEmpty()) pendingCredentialReview.replace(underReview)
                val storedEntryIds = resolveNewStoreEntryIds(idsBefore, storedEntries.count { it != null })
                Napier.d("loadCredentialWithOffer resolved storeEntryIds=$storedEntryIds")
                StoredCredentialIssuanceResult(
                    credentialIssuanceResult = result,
                    storedEntryIds = storedEntryIds,
                    needsReview = storedEntries.any { it == null },
                )
            }
        }
    }

    /** Issued credentials that wait for the user to decide whether to store them, see [keepCredentialsUnderReview]. */
    val credentialsUnderReview: StateFlow<List<PendingCredentialReview.Entry>>
        get() = pendingCredentialReview.entries

    /** Stores every credential under review, as the user decided to keep it although it has been rejected. */
    @Throws(Throwable::class)
    suspend fun keepCredentialsUnderReview(): List<StoreEntryId> {
        val idsBefore = subjectCredentialStore.observeStoreContainer().first().credentials.map { it.first }.toSet()
        val entries = pendingCredentialReview.take()
        entries.forEach { storeAfterUserConsent(it.prepared) }
        return resolveNewStoreEntryIds(idsBefore, entries.size)
    }

    /** Drops every credential under review, as the user decided not to keep it. */
    fun discardCredentialsUnderReview() = pendingCredentialReview.clear()

    /**
     * Validates [credential] as the type of [scheme] under [ValidationPolicies.issuance] with the key and validator of
     * the [holderAgent], see [ReceivedCredential.prepareCredentialStorage], and stores it if it is accepted, returning
     * its entry. A credential rejected only for problems the user may tolerate, e.g. an
     * untrusted issuer, an expired one, an unknown status, or claims its type requires that the issuer left out, waits
     * in [underReview] if it is set, so `null` is returned; otherwise, e.g. when refreshing a
     * credential in the background, it is stored nevertheless, as before. A credential failing a check of its
     * integrity, e.g. its signature or its binding to the key of the wallet, or one that can not be decoded at all,
     * fails with that check, see [integrityRejection]. Every problem of a rejected credential is logged.
     */
    private suspend fun storeCredential(
        credential: ReceivedCredential,
        scheme: CredentialScheme,
        renewalInfo: CredentialRenewalInfo? = null,
        underReview: MutableList<PendingCredentialReview.Entry>?,
    ): KmmResult<SubjectCredentialStore.StoreEntry?> = catching {
        val prepared = withContext(Dispatchers.Default) {
            credential.prepareCredentialStorage(holderAgent, scheme, ValidationPolicies.issuance, renewalInfo)
                .getOrThrow()
        }
        val report = prepared.validation.report
        if (report.decision == ValidationDecision.ACCEPTED) {
            return@catching withContext(Dispatchers.Default) { holderAgent.storeAccepted(prepared).getOrThrow() }
                .storedEntry ?: throw IllegalStateException("The credential has not been stored")
        }
        Napier.w("Issued credential rejected: ${report.describeProblems()}")
        val outcome = report.toUi { scheme.claimLabel(it) }
        when {
            outcome.integrityFailed || prepared.validation.unacceptedCredential == null ->
                throw report.integrityRejection() ?: report.rejection()
                    ?: IllegalStateException("The credential has not been stored")

            underReview != null && report.needsReview() -> null.also {
                underReview += PendingCredentialReview.Entry(prepared, scheme, outcome)
            }

            else -> storeAfterUserConsent(prepared)
        }
    }

    /** Stores [prepared] although it has been rejected, as long as it is intact, see [HolderAgent.storeAfterUserConsent]. */
    private suspend fun storeAfterUserConsent(prepared: PreparedCredentialStorage): SubjectCredentialStore.StoreEntry =
        withContext(Dispatchers.Default) { holderAgent.storeAfterUserConsent(prepared).getOrThrow() }.storedEntry
            ?: throw prepared.validation.report.rejection()
                ?: IllegalStateException("The credential has not been stored")

    /**
     * Every credential of [CredentialIssuanceResult.Success.issued], with the configuration it has been issued for,
     * i.e. the scheme it has been requested as and its renewal info.
     */
    private val CredentialIssuanceResult.Success.credentialsByConfiguration: List<Pair<IssuedCredentials, ReceivedCredential>>
        get() = issued.flatMap { issued -> issued.receivedCredentials.map { issued to it } }

    /**
     * Waits for the store to emit a container with all [count] entries stored since [idsBefore], as the session's hot
     * store may still show only some of them, returning those found so far on a timeout.
     */
    private suspend fun resolveNewStoreEntryIds(idsBefore: Set<StoreEntryId>, count: Int): List<StoreEntryId> {
        if (count == 0) return emptyList()
        var newIds = emptyList<StoreEntryId>()
        return withTimeoutOrNull(3_000.milliseconds) {
            subjectCredentialStore.observeStoreContainer()
                .map { container -> container.credentials.map { it.first }.filterNot { it in idsBefore } }
                .onEach { newIds = it }
                .first { it.size >= count }
        } ?: run {
            Napier.w("resolveNewStoreEntryIds timed out waiting for $count new store entries, found ${newIds.size}")
            newIds
        }
    }

}

internal fun validateProvisioningCallbackStateValue(callbackState: String, expectedState: String) {
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
