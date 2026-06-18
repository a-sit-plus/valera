package ui.viewmodels.authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialToJsonConverter
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.jsonObject

class AuthenticationSelectionPresentationExchangeViewModel(
    val credentialMatchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
    val confirmSelections: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
    val navigateUp: () -> Unit,
) {
    val requests: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>> =
        credentialMatchingResult.matchingResult.inputDescriptorMatches

    val requestIterator = mutableStateOf(0)
    val iterableRequests = requests.toList()
    var attributeSelection: SnapshotStateMap<String, SnapshotStateMap<String, Boolean>> = mutableStateMapOf()
    var credentialSelection: SnapshotStateMap<String, MutableState<SubjectCredentialStore.StoreEntry>> =
        mutableStateMapOf()

    init {
        requests.forEach {
            attributeSelection[it.key] = mutableStateMapOf()
            val matchingCredentials = it.value
            val defaultCredential = matchingCredentials.keys.firstOrNull()
                ?: throw IllegalStateException(
                    "Presentation definition input descriptor '${it.key}' did not match any stored credential"
                )
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
            @Suppress("DEPRECATION") val submission = requests.mapNotNull { (requestsId, matches) ->
                val credential = credentialSelection[requestsId]?.value ?: return@mapNotNull null
                val constraints = matches[credential]?.filter { it.value.isNotEmpty() } ?: return@mapNotNull null
                val attributes = attributeSelection[requestsId] ?: return@mapNotNull null
                val disclosedAttributeSelection = constraints.mapNotNull { constraint ->
                    val path = constraint.value.firstOrNull()?.normalizedJsonPath
                    val memberName = (path?.segments?.last() as NormalizedJsonPathSegment.NameSegment).memberName
                    if (attributes[memberName] == true) {
                        path
                    } else {
                        null
                    }
                }
                requestsId to PresentationExchangeCredentialDisclosure(
                    credential,
                    disclosedAttributeSelection
                )
            }.toMap()
            Napier.d("Presenting Selection: $submission")
            confirmSelections(PresentationExchangeCredentialSubmissions(submission))
        }
    }
}
