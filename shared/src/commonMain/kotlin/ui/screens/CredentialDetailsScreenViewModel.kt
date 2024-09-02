package ui.screens

import at.asitplus.wallet.app.common.WalletMain
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CredentialDetailsScreenViewModel(
    val storeEntryId: StoreEntryId,
    val walletMain: WalletMain,
) {
    val storeEntry = walletMain.subjectCredentialStore.observeStoreContainer().map { container ->
        container.credentials.find {
            it.first == storeEntryId
        }?.second
    }

    fun deleteStoreEntry() {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}