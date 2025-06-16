package ui.models

sealed interface CredentialFreshnessValidationStateUiModel {
    data object Loading : CredentialFreshnessValidationStateUiModel
    data class Done(
        val credentialFreshnessSummary: CredentialFreshnessSummaryUiModel
    ) : CredentialFreshnessValidationStateUiModel
}

