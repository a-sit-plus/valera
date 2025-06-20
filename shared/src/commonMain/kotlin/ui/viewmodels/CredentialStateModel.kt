package ui.viewmodels

import at.asitplus.wallet.lib.agent.SubjectCredentialStore

sealed interface CredentialStateModel {
    data object Loading : CredentialStateModel
    data class Success(
        val credentials: List<Pair<Long, SubjectCredentialStore.StoreEntry>>,
    ) : CredentialStateModel
}