package ui.viewmodels

import data.credentials.CredentialAdapter
import ui.models.ResolvedCredential

sealed interface CredentialStateModel {
    data object Loading : CredentialStateModel
    data class Success(
        val credentials: List<Pair<Long, CredentialListItemUiModel>>,
    ) : CredentialStateModel
}

data class CredentialListItemUiModel(
    val credential: ResolvedCredential,
    val summaryAdapter: CredentialAdapter?,
)
