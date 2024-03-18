package at.asitplus.wallet.app.common

import BuildContext
import ErrorService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    val buildContext: BuildContext,
    val errorService: ErrorService = ErrorService(mutableStateOf(false), mutableStateOf(null)),
) {
    lateinit var walletConfig: WalletConfig
    private lateinit var cryptoService: CryptoService
    lateinit var subjectCredentialStore: PersistentSubjectCredentialStore
    private lateinit var holderAgent: HolderAgent
    private lateinit var holderKeyService: HolderKeyService
    lateinit var provisioningService: ProvisioningService
    lateinit var httpService: HttpService
    lateinit var presentationService: PresentationService
    lateinit var snackbarService: SnackbarService
    private val regex = Regex("^(?=\\[[0-9]{2})", option = RegexOption.MULTILINE)
    val scope = CoroutineScope(Dispatchers.Default)

    init {
        at.asitplus.wallet.idaustria.Initializer.initWithVcLib()
        Napier.takeLogarithm()
        Napier.base(AntilogAdapter(platformAdapter, ""))
    }

    @Throws(Throwable::class)
    fun initialize(snackbarService: SnackbarService) {
        walletConfig =
            WalletConfig(dataStoreService = this.dataStoreService, errorService = errorService)
        cryptoService = objectFactory.loadCryptoService().getOrThrow()
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)
        holderAgent = HolderAgent.newDefaultInstance(
            cryptoService = cryptoService,
            subjectCredentialStore = subjectCredentialStore,
        )
        holderKeyService = objectFactory.loadHolderKeyService().getOrThrow()
        httpService = HttpService()
        provisioningService = ProvisioningService(
            platformAdapter,
            dataStoreService,
            cryptoService,
            holderAgent,
            walletConfig,
            errorService,
            httpService
        )
        presentationService = PresentationService(
            platformAdapter,
            cryptoService,
            holderAgent,
            httpService
        )
        this.snackbarService = snackbarService
    }

    suspend fun resetApp() {
        dataStoreService.clearLog()

        subjectCredentialStore.reset()

        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_XAUTH)
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_COOKIES)
        walletConfig.reset()
    }

    fun getLog(): List<String> {
        val rawLog = platformAdapter.readFromFile("log.txt", "logs")
        return if (rawLog != null) {
            rawLog.split(regex = regex).filter { it.isNotEmpty() }
        } else {
            listOf("")
        }
    }
}

/**
 * Factory to call back to native code to create service objects needed in [WalletMain].
 *
 * Especially useful to call back to Swift code, i.e. to create a [CryptoService] based
 * on Apple's CryptoKit.
 *
 * Most methods are suspending to be able to use biometric authentication or show some other
 * dialogs. Also return `KmmResult` to be able to transport exceptions across system boundaries
 * efficiently.
 */
interface ObjectFactory {
    fun loadCryptoService(): KmmResult<CryptoService>
    fun loadHolderKeyService(): KmmResult<HolderKeyService>
}

/**
 * Interface which defines native keychain callbacks for the ID Holder
 */
interface HolderKeyService {
    /**
     * Clears the private and public key from the keychain/keystore
     */
    fun clear()
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
     * Converts an image from ByteArray to ImageBitmap
     * @param image the image as ByteArray
     * @return returns the image as an ImageBitmap
     */
    fun decodeImage(image: ByteArray): ImageBitmap

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
     * Exits the app in the event of an uncorrectable error
     */
    fun exitApp()

    /**
     * Opens the platform specific share dialog
     */
    fun shareLog()
}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        return ImageBitmap(0, 0)
    }

    override fun writeToFile(text: String, fileName: String, folderName: String) {
    }

    override fun readFromFile(fileName: String, folderName: String): String? {
        return null
    }

    override fun clearFile(fileName: String, folderName: String) {
    }

    override fun exitApp() {
    }

    override fun shareLog() {
    }

}
