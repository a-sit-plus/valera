package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.data.CredentialScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface WalletSubjectCredentialStore : SubjectCredentialStore {
    suspend fun reset()

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId)

    /**
     * The ID that [entry] has been stored with by this store instance recently, e.g. as returned by
     * [at.asitplus.wallet.lib.agent.HolderAgent.storeAccepted], without waiting for [observeStoreContainer] to show it.
     */
    fun storeEntryIdOf(entry: StoreEntry): StoreEntryId? = null

    override suspend fun getCredentials(
        credentialSchemes: Collection<CredentialScheme>?,
    ): KmmResult<List<StoreEntry>> {
        val latestCredentials = observeStoreContainer().first().credentials.map { it.second }
        return KmmResult.success(
            credentialSchemes?.let { schemes ->
                latestCredentials.filter { it.resolveScheme() in schemes }
            } ?: latestCredentials,
        )
    }

    fun observeStoreContainer(): Flow<StoreContainer>
}
