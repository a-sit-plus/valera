package ui.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.navigation.routes.CredentialDetailsRoute

class CredentialDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    val walletMain: WalletMain,
): ViewModel() {
    val storeEntryId = savedStateHandle.toRoute<CredentialDetailsRoute>().storeEntryId
    val imageDecoder: (ByteArray) -> ImageBitmap = { byteArray ->
        walletMain.platformAdapter.decodeImage(byteArray)
    }

    val storeEntry = walletMain.hotStoreContainer.map { uiState ->
        uiState.map { container ->
            container.credentials.find {
                it.first == storeEntryId
            }?.second
        }
    }.stateIn(
        scope = viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState.Loading()
    )

    fun deleteStoreEntry() {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}