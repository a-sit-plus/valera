package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.DigitalCredentialInterface
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.dcapi.DCAPIIssuingRequest
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.PersistentTrustListStore
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.swiftzer.semver.SemVer
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.getString
import org.multipaz.prompt.PromptModel

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val keyMaterial: WalletKeyMaterial,
    val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    val subjectCredentialStore: WalletSubjectCredentialStore,
    val buildContext: BuildContext,
    promptModel: PromptModel,
    val credentialValidator: Validator,
    val holderAgent: HolderAgent,
    val provisioningService: ProvisioningService,
    val httpService: HttpService,
    val presentationService: PresentationService,
    val signingService: SigningService,
    val dcApiExportService: DCAPIExportService,
    val errorService: ErrorService,
    val loadingStatusService: LoadingStatusService,
    val snackbarService: SnackbarService,
    val settingsRepository: SettingsRepository,
    val localPresentmentSessionCoordinator: LocalPresentmentSessionCoordinator,
    val sessionService: SessionService,
    val capabilitiesService: CapabilitiesService,
    val credentialValidityService: CredentialValidityService,
    val attestationService: AttestationService,
    sessionCoroutineScope: CoroutineScope,
    val trustListService: TrustListService,
) {
    val appReady = MutableStateFlow<Boolean?>(null)

    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        errorService.emit(error)
    }
    val scope =
        CoroutineScope(
            sessionCoroutineScope.coroutineContext + coroutineExceptionHandler + promptModel + CoroutineName(
                "WalletMain"
            )
    )
    private var dcApiRegistrationJob: Job? = null

    init {
        resolveUnknownCredentialSchemes()
        credentialValidityService.startChecking()
        trustListService.startChecking()
        if (keyMaterial.keyMaterial is FallBackKeyMaterial) {
            Napier.e("FallBackKeyMaterial: ${keyMaterial.keyMaterial.reason}")
        }
    }

    /**
     * Resolves the scheme of every stored credential on startup, triggering remote type-metadata retrieval for
     * those whose scheme is not known locally. Resolved schemes are cached in VC-K for later [resolveScheme] calls.
     */
    private fun resolveUnknownCredentialSchemes() {
        scope.launch(Dispatchers.IO) {
            catchingUnwrapped {
                subjectCredentialStore.observeStoreContainer().first().credentials.forEach { (_, entry) ->
                    catchingUnwrapped { entry.resolveScheme() }
                }
            }
        }
    }

    suspend fun resetApp() {
        Napier.d("Perform full reset")
        dataStoreService.clearLog()
        subjectCredentialStore.reset()
        signingService.reset()
        capabilitiesService.reset()
        localPresentmentSessionCoordinator.resetAll("wallet-reset")

        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT_BY_STATE)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_INSTANCE_ATTESTATION_BY_STATE)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_ACTIVE_PROVISIONING_STATE)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_STATE_TO_CODE_STORE)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_COOKIES)
        KeystoreService.clearKeyMaterial()

        settingsRepository.reset()
        appReady.value = false
        sessionService.newScope()
    }

    fun softReset() {
        Napier.d("Perform soft reset")
        appReady.value = false
        KeystoreService.clearKeyMaterial()
        localPresentmentSessionCoordinator.resetAll("wallet-soft-reset")
        sessionService.newScope()
    }

    fun getLog(): List<String> {
        val rawLog = platformAdapter.readFromFile("log.txt", "logs")
        return rawLog?.split(regex = regex)?.filter { it.isNotEmpty() } ?: listOf("")
    }

    fun clearLog() {
        dataStoreService.clearLog()
    }

    fun startProvisioning(
        host: String,
        credentialIdentifierInfo: CredentialIdentifierInfo,
        onSuccess: () -> Unit,
    ) {
        scope.launch {
            try {
                provisioningService.startProvisioningWithAuthRequest(
                    credentialIssuer = host,
                    credentialIdentifierInfo = credentialIdentifierInfo,
                )
                onSuccess()
            } catch (e: Throwable) {
                errorService.emit(e)
            }
        }
    }

    fun startDcApiCredentialRegistration() {
        if (dcApiRegistrationJob != null) {
            return
        }
        try {
            Napier.d("DC API: Starting to observe credentials")
            dcApiRegistrationJob = subjectCredentialStore.observeStoreContainer().onEach { storeContainer ->
                dcApiExportService.registerCredentialWithSystem(storeContainer, scope)
            }.launchIn(scope)
        } catch (e: Throwable) {
            Napier.w("DC API: Could not update credentials with system", e)
        }
    }

    suspend fun refreshDcApiCredentialRegistration() {
        try {
            Napier.d("DC API: Refreshing credential registration")
            val storeContainer = subjectCredentialStore.observeStoreContainer().first()
            dcApiExportService.registerCredentialWithSystem(storeContainer, scope)
        } catch (e: Throwable) {
            Napier.w("DC API: Could not refresh credentials with system", e)
        }
    }

    fun updateCheck() {
        scope.launch(Dispatchers.IO) {
            catchingUnwrapped {
                val host = "https://wallet.a-sit.plus/"
                val url = "${host}check.json"
                Napier.d("Performing update check with $url")
                val json = httpService.cachedResourceClient(dataStoreService).get(url).body<JsonObject>()
                json["apps"]?.jsonObject?.get(buildContext.packageName)?.let {
                    (it as? JsonObject)?.get("latestVersion")?.jsonPrimitive?.content?.let {
                        val latestVersion = SemVer.parse(it)
                        val currentVersion = SemVer.parse(buildContext.versionName)
                        Napier.d("Version is $currentVersion, latest is $latestVersion")
                        if (latestVersion > currentVersion) {
                            snackbarService.showSnackbar(
                                getString(Res.string.snackbar_update_hint, latestVersion),
                                getString(Res.string.snackbar_update_action)
                            ) {
                                platformAdapter.openUrl(host)
                            }
                        }
                    }
                }
            }.onFailure {
                Napier.w("Update check failed", it)
            }
        }
    }

    suspend fun checkCredentialFreshness(storeEntry: SubjectCredentialStore.StoreEntry) = when (val it = storeEntry) {
        is SubjectCredentialStore.StoreEntry.Iso -> credentialValidator.checkCredentialFreshness(it.issuerSigned)
        is SubjectCredentialStore.StoreEntry.SdJwt -> credentialValidator.checkCredentialFreshness(it.sdJwt)
        is SubjectCredentialStore.StoreEntry.Vc -> credentialValidator.checkCredentialFreshness(it.vc)
    }
}

fun PlatformAdapter.decodeImage(image: ByteArray) = catchingUnwrapped {
    image.decodeToImageBitmap()
}


/**
 * Adapter to call back to native code without the need for service objects
 */
interface PlatformAdapter {
    /**
     * Opens a specified resource (Intent, Associated Domain)
     */
    fun openUrl(url: String)

    /**
     * Tries to hand a FIDO hybrid-transport QR-code URI to a system component.
     *
     * @return whether a platform handler was started
     */
    fun tryOpenCrossDeviceQrCode(uri: String): Boolean

    /**
     * Writes a user defined string to a file in a specific folder
     * @param text is the content of the new file or the content which gets append to an existing file
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     */
    fun writeToFile(text: String, fileName: String, folderName: String)

    /**
     * Reads the content from a file in a specific folder
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     * @return returns the content of the file
     */
    fun readFromFile(fileName: String, folderName: String): String?

    /**
     * Clears the content of a file
     * @param fileName the name of the file
     * @param folderName the name of the folder in which the file resides
     */
    fun clearFile(fileName: String, folderName: String)

    /**
     * Opens the platform-specific share dialog
     */
    fun shareLog()

    /**
     * Registers credentials with the digital credentials browser API
     * @param entries credentials to add
     * @param scope CoroutineScope for registering credentials
     */
    suspend fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope)

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIVerificationData(): KmmResult<RequestParametersFrom.DcApiRequest>

    /**
     * Retrieves creation request from the digital credentials browser API
     */
    fun getCurrentDCAPIIssuingData(): KmmResult<DCAPIIssuingRequest>

    /** Returns a successful Digital Credentials API response to the invoker. */
    fun prepareDCAPICredentialResponse(response: DigitalCredentialInterface)

    /** Returns a Digital Credentials API error to the invoker. */
    fun prepareDCAPICredentialError(error: String)

    /**
     * Returns a Digital Credentials API issuing response to the invoker.
     *
     * @param response serialized issuing result payload
     * @param success whether issuing completed successfully
     */
    fun prepareDCAPIIssuingResponse(response: String, success: Boolean)

    /**
     * Indicates whether there is an active DC API issuing request waiting for a response.
     */
    fun hasPendingDCAPIIssuingRequest(): Boolean

    /**
     * Opens the platform's application/system settings screen.
     */
    fun openDeviceSettings()

    /**
     * Returns current camera permission state.
     *
     * @return `true` if granted, `false` if denied/restricted, `null` if not determined yet
     */
    fun getCameraPermission(): Boolean?

}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
    }

    override fun tryOpenCrossDeviceQrCode(uri: String): Boolean = false

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun shareLog() {
    }

    override suspend fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope) {
    }

    override fun getCurrentDCAPIVerificationData(): KmmResult<RequestParametersFrom.DcApiRequest> {
        return KmmResult.failure(IllegalStateException("Using dummy platform adapter"))
    }

    override fun getCurrentDCAPIIssuingData(): KmmResult<DCAPIIssuingRequest> {
        return KmmResult.failure(IllegalStateException("Using dummy platform adapter"))
    }

    override fun prepareDCAPICredentialResponse(response: DigitalCredentialInterface) {
    }

    override fun prepareDCAPICredentialError(error: String) {
    }

    override fun prepareDCAPIIssuingResponse(response: String, success: Boolean) {
    }

    override fun hasPendingDCAPIIssuingRequest(): Boolean {
        return false
    }

    override fun openDeviceSettings() {
    }

    override fun getCameraPermission(): Boolean? {
        return false
    }

}
