package ui.viewmodels.Authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

class AuthenticationSelectionViewModel(
    val walletMain: WalletMain,
    val requests: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>>,
    val confirmSelections: (Map<String, CredentialSubmission>) -> Unit,
    val navigateUp: () -> Unit
) {
    val requestIterator = mutableStateOf(0)
    val iterableRequests = requests.toList()
    var attributeSelection: SnapshotStateMap<String, SnapshotStateMap<NormalizedJsonPath, Boolean>> =
        mutableStateMapOf()
    var credentialSelection: SnapshotStateMap<String, MutableState<SubjectCredentialStore.StoreEntry>> =
        mutableStateMapOf()

    init {
        requests.forEach {
            attributeSelection[it.key] = mutableStateMapOf()
            val matchingCredentials = it.value
            val defaultCredential = matchingCredentials.keys.first()
            credentialSelection[it.key] = mutableStateOf(defaultCredential)
        }
    }

    val onBack = {
        if (requestIterator.value > 0) {
            requestIterator.value -= 1
        } else {
            navigateUp()
        }
    }

    val onNext = {
        if (requestIterator.value < requests.size - 1) {
            requestIterator.value += 1
        } else {
            val submission = requests.mapNotNull {
                val requestsId = it.key
                val credential = credentialSelection[requestsId] ?: return@mapNotNull null
                val attributes = attributeSelection[requestsId]?.filter { it.value == true }?.keys
                    ?: return@mapNotNull null
                Pair(requestsId, CredentialSubmission(credential.value, attributes))
            }.toMap()
            confirmSelections(submission)
        }
    }
}