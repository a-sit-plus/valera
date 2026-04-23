package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_fiissuer_request_submitted
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.fiissuer.FIIssuerCredentialTypeDto
import at.asitplus.wallet.app.common.fiissuer.FIIssuerService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class LoadWithFIIssuerViewModel(
    private val walletMain: WalletMain,
    private val fiIssuerService: FIIssuerService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoadWithFIIssuerUiState())
    val uiState: StateFlow<LoadWithFIIssuerUiState> = _uiState.asStateFlow()

    init {
        loadCredentialTypes()
    }

    fun loadCredentialTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                fiIssuerService.listCredentialTypeNames()
            }.onSuccess { names ->
                val selected = names.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    credentialTypeNames = names,
                    selectedCredentialType = selected,
                    selectedCredentialTypeDetails = null,
                    claimValues = emptyMap(),
                )
                selected?.let { loadCredentialTypeDetails(it) }
            }.onFailure { error ->
                Napier.w("FIIssuer: failed to load credential type names", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: error::class.simpleName ?: "Failed to load credential types",
                )
            }
        }
    }

    fun selectCredentialType(typeName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCredentialType = typeName,
                selectedCredentialTypeDetails = null,
                claimValues = emptyMap(),
                errorMessage = null,
            )
            loadCredentialTypeDetails(typeName)
        }
    }

    fun updateClaimValue(key: String, value: String) {
        _uiState.value = _uiState.value.copy(
            claimValues = _uiState.value.claimValues + (key to value),
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val selectedType = _uiState.value.selectedCredentialType
        val claimValues = _uiState.value.claimValues
        if (selectedType.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a credential type")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching {
                fiIssuerService.createCredentialRequest(
                    credentialType = selectedType,
                    claims = claimValues,
                )
            }.onSuccess { response ->
                Napier.d("FIIssuer: created credential request transactionId=${response.transactionId} status=${response.status}")
                walletMain.fiIssuerPollingService.trackPendingRequest(
                    transactionId = response.transactionId,
                    credentialType = selectedType,
                    claims = claimValues,
                )
                walletMain.snackbarService.showSnackbar(getString(Res.string.snackbar_fiissuer_request_submitted))
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onSuccess()
            }.onFailure { error ->
                Napier.w("FIIssuer: failed to create credential request", error)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: error::class.simpleName ?: "Failed to submit FIIssuer request",
                )
            }
        }
    }

    private suspend fun loadCredentialTypeDetails(typeName: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            fiIssuerService.getCredentialType(typeName)
        }.onSuccess { details ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedCredentialTypeDetails = details,
                claimValues = details.requiredClaimKeys.associateWith { _uiState.value.claimValues[it] ?: "" },
            )
        }.onFailure { error ->
            Napier.w("FIIssuer: failed to load credential type details for $typeName", error)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error.message ?: error::class.simpleName ?: "Failed to load credential type",
            )
        }
    }
}

data class LoadWithFIIssuerUiState(
    val isLoading: Boolean = true,
    val credentialTypeNames: List<String> = emptyList(),
    val selectedCredentialType: String? = null,
    val selectedCredentialTypeDetails: FIIssuerCredentialTypeDto? = null,
    val claimValues: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)



