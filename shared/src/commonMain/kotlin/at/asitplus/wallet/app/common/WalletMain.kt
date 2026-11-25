package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList
import at.asitplus.dcapi.DigitalCredentialInterface
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_store_update_action
import at.asitplus.valera.resources.snackbar_store_update_hint
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.attestation.AttestationService
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.dcapi.DCAPIIssuingRequest
import at.asitplus.wallet.app.common.dcapi.DCAPIRegistrationResult
import at.asitplus.wallet.app.common.dcapi.DCAPIVerificationRequest
import at.asitplus.wallet.app.common.dcapi.DigitalCredentialsPermission
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.app.common.presentation.LocalPresentmentSessionCoordinator
import at.asitplus.wallet.app.common.presentation.LongfellowZkRegistration
import at.asitplus.wallet.app.common.validation.StoredCredentialValidator
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.oidvci.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
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
    val storedCredentialValidator: StoredCredentialValidator,
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
    private var dcApiPermissionNoticeShown = false
    private val _dcApiPermissionNotice = MutableStateFlow(false)

    /**
     * Whether to tell the user that credentials could not be registered with the DC API because access was denied.
     * Raised at most once per session, see [consumeDcApiPermissionNotice].
     */
    val dcApiPermissionNotice: StateFlow<Boolean> = _dcApiPermissionNotice.asStateFlow()

    init {
        if (keyMaterial.keyMaterial is FallBackKeyMaterial) {
            Napier.e("FallBackKeyMaterial: ${keyMaterial.keyMaterial.reason}")
        }
    }

    /**
     * Starts the periodic checks of the stored credentials, of the Trust Lists and the WRPRC trust anchors, and loads
     * the ZK circuits ahead of the first presentation, which waits for them. Not started in the iOS Identity Document
     * Provider extension, see [createTransientFlowWalletSessionScope].
     */
    fun startBackgroundWork() {
        credentialValidityService.startChecking()
        trustListService.startChecking()
        scope.launch { LongfellowZkRegistration.ensureRegistered() }
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
        credentialIdentifierInfos: NonEmptyList<CredentialIdentifierInfo>,
        onSuccess: () -> Unit,
    ) {
        launchProvisioningRequest {
            provisioningService.startProvisioningWithAuthRequest(
                credentialIssuer = host,
                credentialIdentifierInfos = credentialIdentifierInfos,
            )
            onSuccess()
        }
    }

    internal fun launchProvisioningRequest(
        onBlocked: () -> Unit = {},
        onFailure: (Throwable) -> Unit = { errorService.emit(it) },
        request: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                request()
            } catch (e: BrowserProvisioningInProgressException) {
                onBlocked()
                credentialValidityService.showBrowserLoginConflict(e, onFailure) {
                    launchProvisioningRequest(onBlocked, onFailure, request)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onFailure(e)
            }
        }
    }

    fun startDcApiCredentialRegistration() {
        if (dcApiRegistrationJob != null) {
            return
        }
        try {
            Napier.d("DC API: Starting to observe credentials")
            // Also registers again when the permission changes, e.g. after the user granted access in the settings
            dcApiRegistrationJob = combine(
                subjectCredentialStore.observeStoreContainer(),
                capabilitiesService.dcApiPermission.distinctUntilChanged(),
            ) { storeContainer, _ -> storeContainer }.onEach { storeContainer ->
                handleDcApiRegistrationResult(dcApiExportService.registerCredentialWithSystem(storeContainer, scope))
            }.launchIn(scope)
        } catch (e: Throwable) {
            Napier.w("DC API: Could not update credentials with system", e)
        }
    }

    suspend fun refreshDcApiCredentialRegistration() {
        try {
            Napier.d("DC API: Refreshing credential registration")
            val storeContainer = subjectCredentialStore.observeStoreContainer().first()
            handleDcApiRegistrationResult(dcApiExportService.registerCredentialWithSystem(storeContainer, scope))
        } catch (e: Throwable) {
            Napier.w("DC API: Could not refresh credentials with system", e)
        }
    }

    fun consumeDcApiPermissionNotice() {
        _dcApiPermissionNotice.value = false
    }

    private fun handleDcApiRegistrationResult(result: DCAPIRegistrationResult) {
        if (result == DCAPIRegistrationResult.PERMISSION_DENIED && !dcApiPermissionNoticeShown) {
            Napier.i("DC API: Access denied, notifying the user")
            dcApiPermissionNoticeShown = true
            _dcApiPermissionNotice.value = true
        }
    }

    /**
     * Asks the app store this app was installed from for an update, or, if it was not installed from one that can be
     * asked, e.g. when sideloaded, checks the latest version announced in `check.json`.
     */
    fun updateCheck() {
        scope.launch(Dispatchers.IO) {
            catchingUnwrapped {
                when (platformAdapter.checkAppStoreUpdate()) {
                    AppStoreUpdateStatus.AVAILABLE -> {
                        Napier.d("App store offers an update")
                        snackbarService.showSnackbar(
                            getString(Res.string.snackbar_store_update_hint),
                            getString(Res.string.snackbar_store_update_action)
                        ) {
                            platformAdapter.startAppStoreUpdate()
                        }
                    }

                    AppStoreUpdateStatus.UP_TO_DATE -> Napier.d("App store offers no update")
                    AppStoreUpdateStatus.UNKNOWN -> announcedUpdateCheck()
                }
            }.onFailure {
                Napier.w("Update check failed", it)
            }
        }
    }

    /**
     * Compares this version with `latestVersion` of this app in `check.json`, and offers to open its `updateUrl`,
     * or the platform's default, e.g. the store listing, if it is newer.
     */
    private suspend fun announcedUpdateCheck() {
        val host = "https://wallet.a-sit.plus/"
        val url = "${host}check.json"
        Napier.d("Performing update check with $url")
        val json = httpService.cachedResourceClient(dataStoreService).get(url).body<JsonObject>()
        val app = json["apps"]?.jsonObject?.get(buildContext.packageName) as? JsonObject ?: return
        val latestVersion = app["latestVersion"]?.jsonPrimitive?.content?.let { SemVer.parse(it) } ?: return
        val updateUrl = app["updateUrl"]?.jsonPrimitive?.contentOrNull
        val currentVersion = SemVer.parse(buildContext.versionName)
        Napier.d("Version is $currentVersion, latest is $latestVersion")
        if (latestVersion > currentVersion) {
            snackbarService.showSnackbar(
                getString(Res.string.snackbar_update_hint, latestVersion),
                getString(Res.string.snackbar_update_action)
            ) {
                platformAdapter.openUpdatePage(url = updateUrl, fallbackUrl = host)
            }
        }
    }

    /** Whether [storeEntry] is expired or not yet valid, and its status, see [StoredCredentialValidator.freshness]. */
    suspend fun checkCredentialFreshness(storeEntry: SubjectCredentialStore.StoreEntry) =
        storedCredentialValidator.freshness(storeEntry)
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
     * @return [DCAPIRegistrationResult.PERMISSION_DENIED] if credentials were not registered because the user
     * denied access
     */
    suspend fun registerWithDigitalCredentialsAPI(
        entries: CredentialRegistry,
        scope: CoroutineScope
    ): DCAPIRegistrationResult

    /**
     * Returns whether the app may register credentials with the digital credentials browser API.
     */
    suspend fun getDigitalCredentialsPermission(): DigitalCredentialsPermission

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIVerificationData(): KmmResult<DCAPIVerificationRequest>

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

    /**
     * Asks the app store this app was installed from, e.g. Google Play, whether it offers an update.
     *
     * @return [AppStoreUpdateStatus.UNKNOWN] if the app was not installed from such a store, e.g. when sideloaded,
     * or the store could not be asked
     */
    suspend fun checkAppStoreUpdate(): AppStoreUpdateStatus = AppStoreUpdateStatus.UNKNOWN

    /**
     * Starts installing the update that [checkAppStoreUpdate] reported as available.
     */
    fun startAppStoreUpdate() {}

    /**
     * Opens a page to update this app when it was not installed from an app store, see [checkAppStoreUpdate].
     *
     * @param url the page announced by the update check, or `null` for the platform's default, e.g. its store listing
     * @param fallbackUrl the page to open if the platform can not open [url] or its default, e.g. without an app store
     */
    fun openUpdatePage(url: String?, fallbackUrl: String) = openUrl(url ?: fallbackUrl)

}

/**
 * Whether the app store this app was installed from offers an update, see [PlatformAdapter.checkAppStoreUpdate].
 */
enum class AppStoreUpdateStatus {
    /** Not installed from an app store that can be asked, or asking it failed */
    UNKNOWN,
    UP_TO_DATE,
    AVAILABLE,
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

    override suspend fun registerWithDigitalCredentialsAPI(
        entries: CredentialRegistry,
        scope: CoroutineScope
    ): DCAPIRegistrationResult = DCAPIRegistrationResult.OK

    override suspend fun getDigitalCredentialsPermission(): DigitalCredentialsPermission =
        DigitalCredentialsPermission.GRANTED

    override fun getCurrentDCAPIVerificationData(): KmmResult<DCAPIVerificationRequest> {
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
