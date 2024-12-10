package ui.viewmodels.Authentication

import androidx.compose.runtime.MutableState
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.RequestOptionParameters

class AuthenticationCredentialSelectionViewModel(
    val walletMain: WalletMain,
    val requests: Map<String, Pair<RequestOptionParameters, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>>>,
    val selectCredentials: (Map<String, SubjectCredentialStore.StoreEntry>) -> Unit,
    val navigateUp: () -> Unit
) {
    val selectedCredential: MutableMap<String, MutableState<SubjectCredentialStore.StoreEntry>> =
        mutableMapOf()
}