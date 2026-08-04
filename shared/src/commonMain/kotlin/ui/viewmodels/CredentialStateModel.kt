package ui.viewmodels

import ui.models.ResolvedCredential

sealed interface CredentialStateModel {
    data object Loading : CredentialStateModel
    data class Success(
        val credentials: List<Pair<Long, ResolvedCredential>>,
    ) : CredentialStateModel
}
