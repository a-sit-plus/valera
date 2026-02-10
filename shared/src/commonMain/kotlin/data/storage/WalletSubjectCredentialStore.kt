package data.storage

import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import kotlinx.coroutines.flow.Flow

interface WalletSubjectCredentialStore {
    suspend fun reset()

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId)

    suspend fun getInvalidCredentials() : List<Pair<Long, StoreEntry>>

    fun observeStoreContainer(): Flow<StoreContainer>
}