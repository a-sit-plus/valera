package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.request.DCAPIRequest
import at.asitplus.iso.EncryptionParameters
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
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
    val snackbarService: SnackbarService,
    val settingsRepository: SettingsRepository,
    val sessionService: SessionService,
    val capabilitiesService: CapabilitiesService,
) {
    val appReady = MutableStateFlow<Boolean?>(null)

    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        errorService.emit(error)
    }
    val scope =
        CoroutineScope(
            Dispatchers.Default + coroutineExceptionHandler + promptModel + CoroutineName(
                "WalletMain"
            )
        )

    init {
        startListeningForNewCredentialsDCAPI()
        if (keyMaterial.keyMaterial is FallBackKeyMaterial) {
            Napier.e("FallBackKeyMaterial: ${keyMaterial.keyMaterial.reason}")
        }
    }

    suspend fun resetApp() {
        Napier.d("Perform full reset")
        dataStoreService.clearLog()
        subjectCredentialStore.reset()
        signingService.reset()
        capabilitiesService.reset()

        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
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

    private fun startListeningForNewCredentialsDCAPI() {
        try {
            val scope =
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler + CoroutineName("startListeningForNewCredentialsDCAPI"))
            Napier.d("DC API: Starting to observe credentials")
            subjectCredentialStore.observeStoreContainer().onEach { storeContainer ->
                dcApiExportService.registerCredentialWithSystem(storeContainer, scope)
            }.launchIn(scope)
        } catch (e: Throwable) {
            Napier.w("DC API: Could not update credentials with system", e)
        }
    }

    fun updateCheck() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val httpClient = httpService.buildHttpClient()
                val host = "https://wallet.a-sit.at/"
                val url = "${host}check.json"
                Napier.d("Performing update check with $url")
                val json = httpClient.get(url).body<JsonObject>()
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
     * Writes an user defined string to a file in a specific folder
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
     * Opens the platform specific share dialog
     */
    fun shareLog()

    /**
     * Registers credentials with the digital credentials browser API
     * @param entries credentials to add
     * @param scope CoroutineScope for registering credentials
     */
    fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope)

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIData(): KmmResult<DCAPIRequest>

    fun prepareDCAPIIsoMdocCredentialResponse(
        responseJson: ByteArray,
        sessionTranscript: ByteArray,
        encryptionParameters: EncryptionParameters
    )

    fun prepareDCAPIOid4vpCredentialResponse(responseJson: String, success: Boolean)

    fun openDeviceSettings()

    fun getCameraPermission(): Boolean?

}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun shareLog() {
    }

    override fun registerWithDigitalCredentialsAPI(entries: CredentialRegistry, scope: CoroutineScope) {
    }

    override fun getCurrentDCAPIData(): KmmResult<DCAPIRequest> {
        return KmmResult.failure(IllegalStateException("Using dummy platform adapter"))
    }

    override fun prepareDCAPIIsoMdocCredentialResponse(
        responseJson: ByteArray,
        sessionTranscript: ByteArray,
        encryptionParameters: EncryptionParameters
    ) {
    }

    override fun prepareDCAPIOid4vpCredentialResponse(responseJson: String, success: Boolean) {
    }

    override fun openDeviceSettings() {
    }

    override fun getCameraPermission(): Boolean? {
        return false
    }

}
