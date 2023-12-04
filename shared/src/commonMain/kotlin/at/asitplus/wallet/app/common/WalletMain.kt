package at.asitplus.wallet.app.common

import DataStoreService
import Resources
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
    }

    suspend fun resetApp(){
        val credentials = subjectCredentialStore.getVcs()
        credentials.forEach {
            subjectCredentialStore.removeCredential(it.id)
        }
        dataStoreService.deleteData(Resources.DATASTORE_KEY_VCS)
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

interface HolderKeyService {
    fun clear()
}

interface PlatformAdapter {
    fun openUrl(url: String)
}

