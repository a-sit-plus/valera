package ui.viewmodels.authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

class AuthenticationSelectionPresentationExchangeViewModel(
    val walletMain: WalletMain,
    val credentialMatchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
    val confirmSelections: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
    val navigateUp: () -> Unit,
    val onClickLogo: () -> Unit
) {
    val requests: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>>
        = credentialMatchingResult.matchingInputDescriptorCredentials

    val requestIterator = mutableStateOf(0)
    val iterableRequests = requests.toList()
    var attributeSelection: SnapshotStateMap<String, SnapshotStateMap<String, Boolean>> =
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
                val credential = credentialSelection[requestsId]?.value ?: return@mapNotNull null
                val constraints = it.value[credential]?.filter { it.value.isNotEmpty() } ?: return@mapNotNull null
                val attributes = attributeSelection[requestsId] ?: return@mapNotNull null
                val disclosedAttributes = constraints.mapNotNull { constraint ->
                    val path = constraint.value.firstOrNull()?.normalizedJsonPath
                    val memberName = (path?.segments?.last() as NormalizedJsonPathSegment.NameSegment).memberName
                    if (attributes[memberName] == true) {
                        path
                    } else {
                        null
                    }
                }
                Pair(requestsId, PresentationExchangeCredentialDisclosure(credential, disclosedAttributes))
            }.toMap()
            confirmSelections(PresentationExchangeCredentialSubmissions(submission))
        }
    }
}