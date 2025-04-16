package at.asitplus.wallet.app.common

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_update_action
import at.asitplus.valera.resources.snackbar_update_hint
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.dcapi.data.CredentialList
import at.asitplus.wallet.app.common.dcapi.DCAPIService
import at.asitplus.wallet.app.common.dcapi.old.DCAPIRequest
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import getImageDecoder
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.swiftzer.semver.SemVer
import org.jetbrains.compose.resources.getString
import org.multipaz.prompt.PromptModel

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val keyMaterial: WalletKeyMaterial,
    val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore,
    val buildContext: BuildContext,
    val promptModel: PromptModel,
    val credentialValidator: Validator,
    val holderAgent: HolderAgent,
    val provisioningService: ProvisioningService,
    val httpService: HttpService,
    val presentationService: PresentationService,
    val signingService: SigningService,
    val dcApiService: DCAPIService,
    val errorService: ErrorService,
    val snackbarService: SnackbarService,
    val settingsRepository: SettingsRepository,
) {
    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        errorService.emit(error)
    }
    val scope =
        CoroutineScope(Dispatchers.Default + coroutineExceptionHandler + promptModel + CoroutineName("WalletMain"))

    suspend fun resetApp() {
        dataStoreService.clearLog()
        subjectCredentialStore.reset()
        signingService.reset()

        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_PROVISIONING_CONTEXT)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_COOKIES)

        settingsRepository.reset()
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

    fun updateCheck() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val httpClient = httpService.buildHttpClient()
                val host = "https://wallet.a-sit.at/"
                val url = "${host}check.json"
                Napier.d("Getting check.json from $url")
                val json = httpClient.get(url).body<JsonObject>()
                json["apps"]?.jsonObject?.get(buildContext.packageName)?.let {
                    (it as? JsonObject)?.get("latestVersion")?.jsonPrimitive?.content?.let {
                        val latestVersion = SemVer.parse(it)
                        val currentVersion = SemVer.parse(buildContext.versionName)
                        Napier.d("Version is $currentVersion, latest is $latestVersion")
                        if (latestVersion > currentVersion) {
                            snackbarService.showSnackbar(
                                getString(
                                    Res.string.snackbar_update_hint,
                                    host,
                                    latestVersion
                                ), getString(Res.string.snackbar_update_action)
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

    suspend fun checkCredentialFreshness(storeEntry: SubjectCredentialStore.StoreEntry) =  when (val it = storeEntry) {
        is SubjectCredentialStore.StoreEntry.Iso -> credentialValidator.checkCredentialFreshness(it.issuerSigned)
        is SubjectCredentialStore.StoreEntry.SdJwt -> credentialValidator.checkCredentialFreshness(it.sdJwt)
        is SubjectCredentialStore.StoreEntry.Vc -> credentialValidator.checkCredentialFreshness(it.vc)
    }
}

fun PlatformAdapter.decodeImage(image: ByteArray): ImageBitmap {
    return getImageDecoder((image))
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
     */
    fun registerWithDigitalCredentialsAPI(entries: CredentialList, scope: CoroutineScope)

    /**
     * Retrieves request from the digital credentials browser API
     */
    fun getCurrentDCAPIData(): DCAPIRequest?

    /**
     * Prepares the credential response and sends it back to the invoking application
     */
    fun prepareDCAPICredentialResponse(responseJson: ByteArray, dcApiRequest: DCAPIRequest)

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

    override fun registerWithDigitalCredentialsAPI(entries: CredentialList, scope: CoroutineScope) {
    }

    override fun getCurrentDCAPIData(): DCAPIRequest? {
        return null
    }

    override fun prepareDCAPICredentialResponse(
        responseJson: ByteArray,
        dcApiRequest: DCAPIRequest
    ) {
    }

}
