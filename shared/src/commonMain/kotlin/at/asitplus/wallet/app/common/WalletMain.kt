package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.VerifiableCredential
import data.idaustria.Initializer
import data.storage.DummyCredentialDataProvider
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory,
    val dataStoreService: DataStoreService,
) {
    init {
        Initializer.initWithVcLib()
    }
    val cryptoService: CryptoService by lazy { objectFactory.loadCryptoService().getOrThrow()}

    val subjectCredentialStore: PersistentSubjectCredentialStore by lazy { PersistentSubjectCredentialStore(this.dataStoreService) }

    /**
     * Temporary function to create a random credential
     */
    suspend fun setCredentials(){
        val holderAgent: HolderAgent by lazy { HolderAgent.newDefaultInstance(cryptoService = this.cryptoService, subjectCredentialStore =  subjectCredentialStore) }
        runBlocking {
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
}