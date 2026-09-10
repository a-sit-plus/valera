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
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.lib.agent.DeviceRequestCredentialDisclosure
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.labeledDisclosedAttributes
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject
import ui.composables.DisclosedAttribute
import ui.composables.TrustState
import ui.composables.TrustStatusBanner
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.ResolvedCredential
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.viewmodels.authentication.IsoDeviceRequestCredentialSubmissions

@Composable
fun IsoDeviceRequestFinalizationPageContent(
    matchingResult: IsoDeviceRetrievalMatchingResult<SubjectCredentialStore.StoreEntry>,
    credentialFreshnessProviders: List<StateFlow<CredentialFreshnessValidationStateUiModel>>,
    submissions: Collection<DeviceRequestCredentialDisclosure<SubjectCredentialStore.StoreEntry>>,
    trustListService: TrustListService,
    request: RequestParametersFrom<*>,
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    onError: (Throwable) -> Unit,
    onAbort: () -> Unit,
    onSubmit: (IsoDeviceRequestCredentialSubmissions<SubjectCredentialStore.StoreEntry>) -> Unit,
) {
    val orderedSubmissions = submissions.sortedBy { it.docRequestIndex }
    val error = validateIsoSubmissions(matchingResult, orderedSubmissions)
    if (error != null) {
        LaunchedEffect(error) { onError(IllegalStateException(error)) }
        return
    }
    val freshnessByCredential = matchingResult.matchingResult.credentials.zip(credentialFreshnessProviders).toMap()

    PresentationFinalizationPageContent(
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        serviceProviderLocalizedName = serviceProviderLocalizedName,
        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
        onAbort = onAbort,
        trustListService = trustListService,
        request = request,
        onSubmit = { onSubmit(IsoDeviceRequestCredentialSubmissions(orderedSubmissions)) },
    ) {
        orderedSubmissions.forEach { disclosure ->
            Spacer(Modifier.height(8.dp))
            IsoSubmissionSummaryCard(
                disclosure,
                requireNotNull(freshnessByCredential[disclosure.credential]),
                trustListService,
            )
        }
    }
}

private fun validateIsoSubmissions(
    matchingResult: IsoDeviceRetrievalMatchingResult<SubjectCredentialStore.StoreEntry>,
    submissions: List<DeviceRequestCredentialDisclosure<SubjectCredentialStore.StoreEntry>>,
): String? {
    val requestCount = matchingResult.presentationRequest.deviceRequest.docRequests.size
    if (submissions.size != requestCount) return "ISO Device Request requires one credential per document request"
    return submissions.map { it.docRequestIndex }.takeUnless { it == (0 until requestCount).toList() }
        ?.let { "ISO Device Request submissions do not preserve document request order" }
}

@Composable
private fun IsoSubmissionSummaryCard(
    disclosure: DeviceRequestCredentialDisclosure<SubjectCredentialStore.StoreEntry>,
    freshnessProvider: StateFlow<CredentialFreshnessValidationStateUiModel>,
    trustListService: TrustListService,
    decodeToBitmap: ImageDecoder = koinInject(),
) {
    val freshness by freshnessProvider.collectAsState()
    val resolvedCredential by produceState<ResolvedCredential?>(null, disclosure.credential) {
        value = catchingUnwrapped { disclosure.credential.toResolvedCredential() }
            .getOrElse { disclosure.credential.toFallbackResolvedCredential() }
    }
    val displayCredential = resolvedCredential ?: return
    val trustState by trustListService.observeTrustStateForEntry(flowOf(resolvedCredential))
        .collectAsState(initial = TrustState.EVALUATING)
    val labeledAttributes = disclosure.credential.labeledDisclosedAttributes(
        scheme = displayCredential.scheme,
        disclosedAttributes = disclosure.disclosedAttributes,
        decodeImage = { decodeToBitmap(it) },
    )

    CredentialSelectionCardLayout(
        isError = (freshness as? CredentialFreshnessValidationStateUiModel.Done)
            ?.credentialFreshnessSummary?.isNotBad == false,
        onClick = {},
        isSelected = true,
        modifier = Modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = freshness,
            credential = displayCredential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = false,
            matchingException = null,
        )
        TrustStatusBanner(trustState, Modifier.fillMaxWidth().padding(vertical = 12.dp))
        CredentialSummaryCardContent(displayCredential) { decodeToBitmap(it) }
        HorizontalDivider(Modifier.fillMaxWidth())
        Column(Modifier.padding(8.dp).fillMaxWidth()) {
            labeledAttributes.forEach { (label, attribute) ->
                DisclosedAttribute(label, attribute, Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
