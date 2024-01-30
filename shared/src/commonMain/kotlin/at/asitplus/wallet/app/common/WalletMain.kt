package at.asitplus.wallet.app.common

import ErrorService
import Resources
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.data.ConstantIndex
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import data.storage.stringify
import io.github.aakira.napier.Napier

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    val errorService: ErrorService = ErrorService(mutableStateOf(false), mutableStateOf(null))
) {
    lateinit var walletConfig: WalletConfig
    private lateinit var cryptoService: CryptoService
    lateinit var subjectCredentialStore: PersistentSubjectCredentialStore
    private lateinit var holderAgent: HolderAgent
    private lateinit var holderKeyService: HolderKeyService
    lateinit var provisioningService: ProvisioningService
    lateinit var presentationService: PresentationService
    lateinit var snackbarService: SnackbarService
    init {
        at.asitplus.wallet.idaustria.Initializer.initWithVcLib()
        Napier.takeLogarithm()
        Napier.base(AntilogAdapter(dataStoreService, ""))
    }
    @Throws(Throwable::class)
    fun initialize(snackbarService: SnackbarService){
        walletConfig = WalletConfig(dataStoreService = this.dataStoreService, errorService = errorService)
        cryptoService = objectFactory.loadCryptoService().getOrThrow()
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)
        holderAgent = HolderAgent.newDefaultInstance(cryptoService = cryptoService, subjectCredentialStore = subjectCredentialStore)
        holderKeyService = objectFactory.loadHolderKeyService().getOrThrow()
        provisioningService = ProvisioningService(platformAdapter, dataStoreService, cryptoService, holderAgent, walletConfig, errorService)
        presentationService = PresentationService(platformAdapter, dataStoreService, cryptoService, holderAgent, errorService)
        this.snackbarService = snackbarService
    }

    suspend fun resetApp(){
        dataStoreService.clearLog()

        subjectCredentialStore.reset()

        dataStoreService.deletePreference(Resources.DATASTORE_KEY_VCS)
        dataStoreService.deletePreference(Resources.DATASTORE_KEY_XAUTH)
        dataStoreService.deletePreference(Resources.DATASTORE_KEY_COOKIES)
        dataStoreService.deletePreference(Resources.DATASTORE_KEY_CONFIG)
        holderKeyService.clear()
        walletConfig.host = "https://wallet.a-sit.at"
        walletConfig.credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT
        cryptoService = objectFactory.loadCryptoService().getOrThrow()
    }

    fun getLog(): MutableList<String>{
        return dataStoreService.readLogFromFile().stringify()
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

    fun decodeImage(image: ByteArray): ImageBitmap

    fun writeToFile(text: String, fileName: String)

    fun readFromFile(fileName: String): String?

    fun clearFile(fileName: String)

    fun exitApp()

    fun shareLog()
}

class DummyPlatformAdapter : PlatformAdapter {
    override fun openUrl(url: String) {
        TODO("Not yet implemented")
    }

    override fun decodeImage(image: ByteArray): ImageBitmap {
        TODO("Not yet implemented")
    }

    override fun writeToFile(text: String, fileName: String) {
        println(text)
    }

    override fun readFromFile(fileName: String): String? {
        TODO("Not yet implemented")
    }

    override fun clearFile(fileName: String) {
        TODO("Not yet implemented")
    }

    override fun exitApp() {
        TODO("Not yet implemented")
    }

    override fun shareLog() {
        TODO("Not yet implemented")
    }

}
