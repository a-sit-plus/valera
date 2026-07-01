package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.Validator
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.Napier
import org.multipaz.prompt.PromptModel

data class WalletDependencyProvider(
    val keystoreService: KeystoreService,
    val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore =
        PersistentSubjectCredentialStore(dataStoreService, Validator()),
    val buildContext: BuildContext,
    val promptModel: PromptModel,
    val antilog: Antilog,
) {
    init {
        registerCredentialMetadata(buildContext, dataStoreService)

        Napier.takeLogarithm()
        Napier.base(antilog)
    }
}
