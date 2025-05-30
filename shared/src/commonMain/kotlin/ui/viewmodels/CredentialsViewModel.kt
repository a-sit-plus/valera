package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import data.storage.StoreContainer
import data.storage.StoreEntryId
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CredentialsViewModel(
    val walletMain: WalletMain,
    val imageDecoder: ImageDecoder,
) : ViewModel() {
    val storeContainer = walletMain.hotStoreContainer

    fun removeStoreEntryById(
        storeEntryId: StoreEntryId,
        completionHandler: CompletionHandler = {},
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
                completionHandler(null)
            } catch (it: Throwable) {
                completionHandler(it)
            }
        }
    }
}

