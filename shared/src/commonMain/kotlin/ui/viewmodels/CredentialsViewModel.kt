package ui.viewmodels

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class CredentialsViewModel(
    val walletMain: WalletMain,
    val imageDecoder: ImageDecoder,
) {
    val storeContainer = walletMain.subjectCredentialStore.observeStoreContainer()

    fun removeStoreEntryById(storeEntryId: StoreEntryId) {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}