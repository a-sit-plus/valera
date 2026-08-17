package ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import data.Attribute
import data.credentials.toCredentialAdapter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject
import ui.composables.LabeledAttribute
import ui.composables.TrustState
import ui.composables.TrustStatusBanner
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.ResolvedCredential
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.viewmodels.authentication.PresentationExchangeCredentialSubmissions

@Composable
fun PresentationExchangeFinalizationPageContent(
    matchingResult: PresentationExchangeMatchingResult<SubjectCredentialStore.StoreEntry>,
    credentialFreshnessProviders: List<StateFlow<CredentialFreshnessValidationStateUiModel>>,
    inputDescriptorSubmissions: Map<String, PresentationExchangeCredentialDisclosure<SubjectCredentialStore.StoreEntry>>,
    trustListService: TrustListService,
    request: RequestParametersFrom<*>,
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    onError: (Throwable) -> Unit,
    onAbort: () -> Unit,
    onSubmit: (PresentationExchangeCredentialSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
) {
    val freshnessProvidersByCredential =
        matchingResult.matchingResult.credentials.zip(credentialFreshnessProviders).toMap()
    var finalizationErrorMessage: String? = null
    val summaryCards = inputDescriptorSubmissions.entries.sortedBy { it.key }.mapNotNull { (inputDescriptorId, disclosure) ->
        val inputDescriptorMatching =
            matchingResult.matchingResult.inputDescriptorMatches[inputDescriptorId]?.get(disclosure.credential)
                ?: run {
                    finalizationErrorMessage =
                        "Failed to find submission data for input descriptor $inputDescriptorId"
                    return@mapNotNull null
                }
        val freshnessProvider = freshnessProvidersByCredential[disclosure.credential]
            ?: run {
                finalizationErrorMessage =
                    "Failed to find freshness provider for input descriptor $inputDescriptorId"
                return@mapNotNull null
            }

        PresentationExchangeSubmissionSummary(
            disclosure = disclosure,
            inputDescriptorMatching = inputDescriptorMatching,
            freshnessProvider = freshnessProvider,
        )
    }
    finalizationErrorMessage?.let { message ->
        LaunchedEffect(message) {
            onError(IllegalStateException(message))
        }
        return
    }

    PresentationFinalizationPageContent(
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        serviceProviderLocalizedName = serviceProviderLocalizedName,
        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
        onAbort = onAbort,
        trustListService = trustListService,
        request = request,
        onSubmit = {
            onSubmit(PresentationExchangeCredentialSubmissions(inputDescriptorSubmissions))
        },
    ) {
        summaryCards.forEach {
            Spacer(modifier = Modifier.height(8.dp))
            PresentationExchangeCredentialSubmissionSummaryCard(
                disclosure = it.disclosure,
                inputDescriptorMatching = it.inputDescriptorMatching,
                freshnessProvider = it.freshnessProvider,
                trustListService = trustListService,
            )
        }
    }
}

private data class PresentationExchangeSubmissionSummary(
    val disclosure: PresentationExchangeCredentialDisclosure<SubjectCredentialStore.StoreEntry>,
    val inputDescriptorMatching: Map<ConstraintField, NodeList>,
    val freshnessProvider: StateFlow<CredentialFreshnessValidationStateUiModel>,
)

@Composable
private fun PresentationExchangeCredentialSubmissionSummaryCard(
    disclosure: PresentationExchangeCredentialDisclosure<SubjectCredentialStore.StoreEntry>,
    inputDescriptorMatching: Map<ConstraintField, NodeList>,
    freshnessProvider: StateFlow<CredentialFreshnessValidationStateUiModel>,
    trustListService: TrustListService,
    decodeToBitmap: ImageDecoder = koinInject(),
) {
    val credential = disclosure.credential
    val credentialFreshnessValidationState by freshnessProvider.collectAsState()
    val resolvedCredential by produceState<ResolvedCredential?>(null, credential) {
        value = catchingUnwrapped { credential.toResolvedCredential() }
            .getOrElse { credential.toFallbackResolvedCredential() }
    }
    val displayCredential = resolvedCredential ?: return
    val trustState by trustListService
        .observeTrustStateForEntry(flowOf(resolvedCredential))
        .collectAsState(initial = TrustState.EVALUATING)
    val credentialAdapter = credential.toCredentialAdapter(displayCredential.scheme) { decodeToBitmap(it) }
    val disclosedPathStrings = disclosure.disclosedAttributes.map { it.toString() }.toSet()

    val labeledAttributes = inputDescriptorMatching.values.mapNotNull { nodeList ->
        nodeList.firstOrNull { it.normalizedJsonPath.toString() in disclosedPathStrings }
    }.mapNotNull { nodeListEntry ->
        val path = nodeListEntry.normalizedJsonPath
        val label = path.segments.lastOrNull()?.let {
            displayCredential.scheme.getLocalization(path)
                ?: displayCredential.scheme.getLocalization(NormalizedJsonPath(it))
        } ?: path.toString()
        val attribute = try {
            credentialAdapter.getAttribute(path)
        } catch (_: Throwable) {
            Attribute.fromValue(nodeListEntry.value)
        } ?: return@mapNotNull null
        label to attribute
    }.sortedBy { it.first }

    CredentialSelectionCardLayout(
        isError = when (val state = credentialFreshnessValidationState) {
            is CredentialFreshnessValidationStateUiModel.Done -> !state.credentialFreshnessSummary.isNotBad
            CredentialFreshnessValidationStateUiModel.Loading -> false
        },
        onClick = {},
        isSelected = true,
        modifier = Modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = credentialFreshnessValidationState,
            matchingException = null,
            credential = displayCredential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = false,
        )
        TrustStatusBanner(
            trustState = trustState,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        CredentialSummaryCardContent(
            credential = displayCredential,
            decodeToBitmap = { decodeToBitmap(it) },
        )
        if (labeledAttributes.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
            ) {
                labeledAttributes.forEach { (label, attribute) ->
                    LabeledAttribute(
                        label = label,
                        attribute = attribute,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}
