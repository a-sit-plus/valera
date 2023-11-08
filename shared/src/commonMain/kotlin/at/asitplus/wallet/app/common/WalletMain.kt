package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import data.idaustria.Initializer
import data.storage.DummyCredentialDataProvider
import data.storage.PersistentSubjectCredentialStore

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory,
    var dataStoreService: DataStoreService,
) {
    lateinit var cryptoService: CryptoService
    lateinit var subjectCredentialStore: PersistentSubjectCredentialStore
    lateinit var holderAgent: HolderAgent
    lateinit var holderKeyService: HolderKeyService

    init {
        Initializer.initWithVcLib()
    }
    fun initialize(){
        cryptoService = objectFactory.loadCryptoService().getOrThrow()
        subjectCredentialStore = PersistentSubjectCredentialStore(this.dataStoreService)
        holderAgent = HolderAgent.newDefaultInstance(cryptoService = this.cryptoService, subjectCredentialStore = subjectCredentialStore)
        holderKeyService = objectFactory.loadHolderKeyService().getOrThrow()
    }

    
    /**
     * Temporary function to create a random credential
     */
    suspend fun setCredentials(){
        holderAgent.storeCredentials(
            IssuerAgent.newDefaultInstance(
                DefaultCryptoService(),
                dataProvider = DummyCredentialDataProvider(),
            ).issueCredentialWithTypes(
                holderAgent.identifier,
                attributeTypes = listOf(data.idaustria.ConstantIndex.IdAustriaCredential.vcType)
            ).toStoreCredentialInput()
        )
    }

    suspend fun resetApp(){
        val credentials = subjectCredentialStore.getVcs()
        credentials.forEach {
            subjectCredentialStore.removeCredential(it.id)
        }
        this.dataStoreService.deleteData("VCs")
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



