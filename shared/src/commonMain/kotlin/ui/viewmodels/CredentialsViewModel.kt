package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.toCredentialFreshnessSummaryModel

class CredentialsViewModel(
    private val walletMain: WalletMain,
    private val imageDecoder: ImageDecoder,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
) : ViewModel() {
    val storeContainer = subjectCredentialStore.observeStoreContainer().map {
        CredentialStateModel.Success(it.credentials)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CredentialStateModel.Loading
    )

    val credentialTimelinessesStates = channelFlow {
        val knownStates = mutableMapOf<Long, CredentialFreshnessSummaryUiModel>()
        subjectCredentialStore.observeStoreContainer().collectLatest {
            coroutineScope {
                it.credentials.forEach {
                    launch {
                        knownStates[it.first] = walletMain.checkCredentialFreshness(it.second)
                            .toCredentialFreshnessSummaryModel()
                        send(knownStates.toMap())
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = mutableMapOf()
    )

    fun decodeImage(byteArray: ByteArray) = imageDecoder(byteArray)

    fun removeStoreEntryById(storeEntryId: StoreEntryId) = viewModelScope.launch {
        subjectCredentialStore.removeStoreEntryById(storeEntryId)
    }
}


