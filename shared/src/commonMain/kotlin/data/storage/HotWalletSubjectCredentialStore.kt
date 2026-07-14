package data.storage

import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

/**
 * This class is used in order to reduce the time needed to load credentials from the store in HolderAgent
 * TODO: Evaluate, whether this takes too much memory or if the performance improvements are worth it
 */
class HotWalletSubjectCredentialStore(
    private val delegate: PersistentSubjectCredentialStore,
    val coroutineScope: CoroutineScope,
) : WalletSubjectCredentialStore, SubjectCredentialStore by delegate {
    override val validator = delegate.validator

    override suspend fun reset() = delegate.reset()

    val hotStoreContainer: StateFlow<StoreContainer?> = delegate.observeStoreContainer().stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override fun observeStoreContainer(): Flow<StoreContainer> = hotStoreContainer.filterNotNull()

    override suspend fun getCredentials(credentialSchemes: Collection<CredentialScheme>?) =
        super.getCredentials(credentialSchemes)

    override suspend fun removeStoreEntryById(
        storeEntryId: StoreEntryId,
    ) = delegate.removeStoreEntryById(storeEntryId)

}
