package ui.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.catchingUnwrapped
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.lib.etsi.LoTEFilterCriteria
import at.asitplus.wallet.lib.etsi.LoTEFilterService
import at.asitplus.wallet.lib.etsi.isTrustedBy
import data.storage.StoreEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.composables.TrustState
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.models.toCredentialFreshnessSummaryModel

class CredentialDetailsViewModel(
    val storeEntryId: StoreEntryId,
    val walletMain: WalletMain,
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit
) : ViewModel() {

    val imageDecoder: (ByteArray) -> Result<ImageBitmap> = { walletMain.platformAdapter.decodeImage(it) }

    val storeEntry = walletMain.subjectCredentialStore.observeStoreContainer().map { container ->
        container.credentials.find {
            it.first == storeEntryId
        }?.second?.let {
            catchingUnwrapped { it.toResolvedCredential() }
                .getOrElse { _ -> it.toFallbackResolvedCredential() }
        }
    }

    val trustState: StateFlow<TrustState> = walletMain.trustListService
        .observeTrustStateForEntry(storeEntry)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrustState.EVALUATING
        )


    val credentialTimelinessesStates = channelFlow {
        val knownStates = mutableMapOf<Long, CredentialFreshnessSummaryUiModel>()
        walletMain.subjectCredentialStore.observeStoreContainer().collectLatest {
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

    fun deleteStoreEntry() {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}
