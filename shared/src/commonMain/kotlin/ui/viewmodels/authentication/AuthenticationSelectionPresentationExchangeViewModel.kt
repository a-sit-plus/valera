package ui.viewmodels.authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialToJsonConverter
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import io.github.aakira.napier.Napier
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
                // Manually assigns all available attributes in ISO credential if only presentation of all attributes shall be supported
                val forcedAttributes = if (credential.representation != ConstantIndex.CredentialRepresentation.ISO_MDOC) {
                    null
                } else when (credential.scheme) {
                    is HealthIdScheme,
                    is EhicScheme,
                    is PowerOfRepresentationScheme,
                    is TaxIdScheme -> {
                        val allAttributes = credential.scheme!!.claimNames.map {
                            NormalizedJsonPath() + credential.scheme!!.isoNamespace!! + it
                        }
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
                    }

                    else -> null
                }

                requestsId to PresentationExchangeCredentialDisclosure(
                    credential,
                    forcedAttributes ?: disclosedAttributeSelection
                )
            }.toMap()
            Napier.d("Presenting Selection: $submission")
            confirmSelections(PresentationExchangeCredentialSubmissions(submission))
        }
    }
}
