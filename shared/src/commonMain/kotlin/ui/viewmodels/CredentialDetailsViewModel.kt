package ui.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.validation.CheckState
import at.asitplus.wallet.app.common.validation.ValidationOutcomeUi
import at.asitplus.wallet.app.common.validation.toUi
import data.storage.StoreEntryId
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ui.composables.TrustState
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.models.toCredentialFreshnessSummaryModel

@OptIn(ExperimentalCoroutinesApi::class)
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

    /** The report of the stored credential, `null` while it is being validated. */
    val validation: StateFlow<StoredCredentialValidation?> = walletMain.subjectCredentialStore.observeStoreContainer()
        .map { container -> container.credentials.find { it.first == storeEntryId }?.second }
        .distinctUntilChanged()
        .mapLatest { entry ->
            StoredCredentialValidation(entry?.let {
                catchingUnwrapped { walletMain.storedCredentialValidator.validate(it).toUi() }
                    .onFailure { Napier.w("Could not validate stored credential $storeEntryId", it) }
                    .getOrNull()
            })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Trust in the issuer, as in the report of the credential, see [validation]. */
    val trustState: StateFlow<TrustState> = validation
        .map { it?.issuerTrustState ?: TrustState.EVALUATING }
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

    /** The [outcome] of validating a stored credential, `null` if it could not be validated at all. */
    data class StoredCredentialValidation(val outcome: ValidationOutcomeUi?) {
        val issuerTrustState: TrustState
            get() = when (outcome?.root?.checks?.firstOrNull { it.name == "issuerTrust" }?.state) {
                CheckState.PASSED -> TrustState.TRUSTED
                CheckState.FAILED -> TrustState.UNTRUSTED
                else -> TrustState.UNKNOWN
            }
    }

    fun deleteStoreEntry() {
        walletMain.scope.launch(Dispatchers.IO) {
            walletMain.subjectCredentialStore.removeStoreEntryById(storeEntryId)
        }
    }
}
