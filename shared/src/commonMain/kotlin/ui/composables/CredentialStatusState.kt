package ui.composables

import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary

sealed interface CredentialStatusState {
    data object Loading : CredentialStatusState
    data class Success(val freshness: CredentialFreshnessSummary?) : CredentialStatusState
}