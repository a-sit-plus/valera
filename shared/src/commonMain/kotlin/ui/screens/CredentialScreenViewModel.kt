package ui.screens

import at.asitplus.wallet.app.common.WalletMain
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class CredentialScreenViewModel(
    val walletMain: WalletMain,
) {
    val storeContainer = walletMain.subjectCredentialStore.observeStoreContainer()

    fun removeCredentialById(storeEntryId: StoreEntryId) {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}