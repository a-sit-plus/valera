package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import data.credentials.toCredentialAdapter
import data.storage.StoreEntryId
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.models.toCredentialFreshnessSummaryModel

class CredentialsViewModel(
    private val walletMain: WalletMain,
    private val imageDecoder: ImageDecoder,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
) : ViewModel() {
    private val credentialLoadingDispatcher = Dispatchers.Default.limitedParallelism(2)

    val storeContainer = subjectCredentialStore.observeStoreContainer().map { container ->
        CredentialStateModel.Success(
            coroutineScope {
                container.credentials.map { (id, entry) ->
                    async {
                        val resolvedCredential = catchingUnwrapped { entry.toResolvedCredential() }
                            .getOrElse { entry.toFallbackResolvedCredential() }
                        val summaryAdapter = if (
                            resolvedCredential.scheme.isEuPid || resolvedCredential.scheme.isMdl
                        ) {
                            catchingUnwrapped {
                                entry.toCredentialAdapter(resolvedCredential.scheme, imageDecoder::invoke)
                            }.getOrNull()
                        } else {
                            null
                        }
                        id to CredentialListItemUiModel(resolvedCredential, summaryAdapter)
                    }
                }.awaitAll()
            }
        )
    }.flowOn(credentialLoadingDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CredentialStateModel.Loading
    )

    val credentialTimelinessesStates = channelFlow {
        val knownStates = mutableMapOf<Long, CredentialFreshnessSummaryUiModel>()
        subjectCredentialStore.observeStoreContainer().collectLatest {
            it.credentials.forEach {
                knownStates[it.first] = walletMain.checkCredentialFreshness(it.second)
                    .toCredentialFreshnessSummaryModel()
                send(knownStates.toMap())
            }
        }
    }.flowOn(credentialLoadingDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = mutableMapOf()
    )

    fun decodeImage(byteArray: ByteArray) = imageDecoder(byteArray)

    fun removeStoreEntryById(storeEntryId: StoreEntryId) = walletMain.scope.launch {
        subjectCredentialStore.removeStoreEntryById(storeEntryId)
    }
}
