package ui.viewmodels.authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.third_party.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialToJsonConverter
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxId2025Scheme
import at.asitplus.wallet.taxid.TaxIdScheme
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

class AuthenticationSelectionPresentationExchangeViewModel(
    val walletMain: WalletMain,
    val credentialMatchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
    val confirmSelections: (CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
    val navigateUp: () -> Unit,
    val navigateToHomeScreen: () -> Unit,
) {
    val requests: Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>> =
        credentialMatchingResult.matchingInputDescriptorCredentials

    val requestIterator = mutableStateOf(0)
    val iterableRequests = requests.toList()
    var attributeSelection: SnapshotStateMap<String, SnapshotStateMap<String, Boolean>> = mutableStateMapOf()
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
            val submission = requests.mapNotNull { (requestsId, matches) ->
                val credential = credentialSelection[requestsId]?.value ?: return@mapNotNull null
                val constraints = matches[credential]?.filter { it.value.isNotEmpty() } ?: return@mapNotNull null
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
                requestsId to PresentationExchangeCredentialDisclosure(
                    credential,
                    // Manually assigns all available attributes in ISO HealthId credential
                    if (credential.representation == ConstantIndex.CredentialRepresentation.ISO_MDOC) {

                        when (credential.scheme) {
                            is HealthIdScheme,
                            is PowerOfRepresentationScheme,
                            is TaxId2025Scheme,
                            is TaxIdScheme -> credential.scheme!!.claimNames.map {
                                NormalizedJsonPath() + credential.scheme!!.isoNamespace!! + it
                            }

                            else -> null
                        }?.let { allAttributes ->
                            val claimStructure = CredentialToJsonConverter.toJsonElement(credential)
                            Napier.d("Claim Structure: $claimStructure")
                            allAttributes.filter { attribute ->
                                val (namespace, attributeName) = attribute.segments.map {
                                    (it as NormalizedJsonPathSegment.NameSegment).memberName
                                }
                                runCatching {
                                    claimStructure.jsonObject[namespace]!!.jsonObject[attributeName] != null
                                }.getOrNull() ?: false
                            }
                        } ?: disclosedAttributes
                    } else disclosedAttributes
                )
            }.toMap()
            Napier.d("Presenting Selection: $submission")
            confirmSelections(PresentationExchangeCredentialSubmissions(submission))
        }
    }
}
