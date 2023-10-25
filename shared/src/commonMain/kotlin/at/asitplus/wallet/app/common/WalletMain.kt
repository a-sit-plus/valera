package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.VerifiableCredential
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
    lateinit var subjectCredentialStore: PersistentSubjectCredentialStore
    suspend fun getCryptoServiceIdentifier(): String {
        val cryptoService = objectFactory.loadCryptoService().getOrElse {
            Napier.w("cryptoService failed", it)
            return "null"
        }
        return cryptoService.identifier
    }

    suspend fun getVcs(): ArrayList<VerifiableCredential> {
        val credentialList = ArrayList<VerifiableCredential>()
        val storeEntries = subjectCredentialStore.getCredentials(null).getOrThrow()
        storeEntries.forEach {entry ->
            when(entry) {
                is SubjectCredentialStore.StoreEntry.Iso -> TODO()
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    credentialList.add(entry.vc.vc)
                }

                else -> {}
            }
        }
        return credentialList
    }

    suspend fun setCredentials(){
        this.objectFactory.loadCryptoService().onSuccess {
            val holderAgent = HolderAgent.newDefaultInstance(cryptoService = it, subjectCredentialStore =  subjectCredentialStore)
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

    suspend fun removeCredentialById(id: String) {
        subjectCredentialStore.removeCredential(id)
    }

    suspend fun getCredentialById(id: String): VerifiableCredential? {
        val storeEntries = subjectCredentialStore.getCredentials(null).getOrThrow()
        storeEntries.forEach {entry ->
            when(entry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    if (entry.vc.vc.id == id){
                        return entry.vc.vc
                    }
                }
                else -> {}
            }
        }
        return null
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
    suspend fun loadCryptoService(): KmmResult<CryptoService>
}