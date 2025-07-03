package ui.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CredentialDetailsViewModel(
    val storeEntryId: StoreEntryId,
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit
) {
    val imageDecoder: (ByteArray) -> Result<ImageBitmap> = { walletMain.platformAdapter.decodeImage(it) }

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