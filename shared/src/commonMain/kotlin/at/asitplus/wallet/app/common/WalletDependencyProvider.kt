package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.rqes.Initializer
import data.storage.AntilogAdapter
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Napier
import org.multipaz.prompt.PromptModel

data class WalletDependencyProvider(
    val keystoreService: KeystoreService,
    val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore =
        PersistentSubjectCredentialStore(dataStoreService),
    val buildContext: BuildContext,
    val promptModel: PromptModel
) {
    init {
        at.asitplus.wallet.mdl.Initializer.initWithVCK()
        at.asitplus.wallet.idaustria.Initializer.initWithVCK()
        at.asitplus.wallet.eupid.Initializer.initWithVCK()
        at.asitplus.wallet.eupidsdjwt.Initializer.initWithVCK()
        at.asitplus.wallet.cor.Initializer.initWithVCK()
        at.asitplus.wallet.por.Initializer.initWithVCK()
        at.asitplus.wallet.companyregistration.Initializer.initWithVCK()
        at.asitplus.wallet.healthid.Initializer.initWithVCK()
        at.asitplus.wallet.taxid.Initializer.initWithVCK()
        at.asitplus.wallet.ehic.Initializer.initWithVCK()

        Initializer.initRqesModule()
        Napier.takeLogarithm()
        Napier.base(AntilogAdapter(platformAdapter, "", buildContext.buildType))
    }
}