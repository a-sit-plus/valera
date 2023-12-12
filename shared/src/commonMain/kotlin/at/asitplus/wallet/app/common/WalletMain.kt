package at.asitplus.wallet.app.common

import DataStoreService
import Resources
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import data.storage.PersistentSubjectCredentialStore

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory,
    private val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter
) {
    private lateinit var cryptoService: CryptoService
    lateinit var subjectCredentialStore: PersistentSubjectCredentialStore
    private lateinit var holderAgent: HolderAgent
    private lateinit var holderKeyService: HolderKeyService
    lateinit var provisioningService: ProvisioningService
    lateinit var presentationService: PresentationService

    init {
        at.asitplus.wallet.idaustria.Initializer.initWithVcLib()
    }
    @Throws(Throwable::class)
    fun initialize(){
        cryptoService = objectFactory.loadCryptoService().getOrThrow()
        subjectCredentialStore = PersistentSubjectCredentialStore(dataStoreService)
        holderAgent = HolderAgent.newDefaultInstance(cryptoService = cryptoService, subjectCredentialStore = subjectCredentialStore)
        holderKeyService = objectFactory.loadHolderKeyService().getOrThrow()
        provisioningService = ProvisioningService(platformAdapter, dataStoreService, cryptoService, holderAgent)
        presentationService = PresentationService(platformAdapter, dataStoreService, cryptoService, holderAgent)
    }

    suspend fun resetApp(){
        subjectCredentialStore.reset()

        dataStoreService.deleteData(Resources.DATASTORE_KEY_VCS)
        dataStoreService.deleteData(Resources.DATASTORE_KEY_XAUTH)
        dataStoreService.deleteData(Resources.DATASTORE_KEY_COOKIES)

        holderKeyService.clear()
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
}

