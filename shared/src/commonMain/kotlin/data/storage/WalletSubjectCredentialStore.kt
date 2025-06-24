package data.storage

import kotlinx.coroutines.flow.Flow

interface WalletSubjectCredentialStore {
    suspend fun reset()

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId)

    fun observeStoreContainer(): Flow<StoreContainer>
}