package ui.composables

sealed interface CredentialFreshnessValidationState {
    data object Loading : CredentialFreshnessValidationState
    data class Done(
        val credentialFreshnessSummaryModel: CredentialFreshnessSummaryModel
    ) : CredentialFreshnessValidationState
}

