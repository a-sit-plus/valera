package ui.views.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_data
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.lib.agent.DeviceRequestCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ui.composables.TrustState
import ui.composables.TrustStatusBanner
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessSummaryModelEvaluator
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.ResolvedCredential
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential
import ui.viewmodels.authentication.AuthenticationSelectionIsoDeviceRequestViewModel

@Composable
fun AuthenticationSelectionIsoDeviceRequestView(
    vm: AuthenticationSelectionIsoDeviceRequestViewModel<SubjectCredentialStore.StoreEntry>,
    onClickLogo: () -> Unit,
    trustListService: TrustListService,
) {
    val requestIndex = vm.requestIterator.value
    val matches = vm.requests[requestIndex]
    AuthenticationSelectionViewScaffold(
        title = stringResource(Res.string.heading_label_select_data),
        onClickLogo = onClickLogo,
        onNavigateUp = vm.onBack,
        onNext = vm.onNext,
    ) {
        LinearProgressIndicator(
            progress = { (requestIndex + 1f) / vm.requests.size },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            matches.forEach { disclosure ->
                IsoDeviceRequestCredentialSelectionCard(
                    disclosure = disclosure,
                    selection = requireNotNull(vm.credentialSelection[requestIndex]),
                    trustListService = trustListService,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun IsoDeviceRequestCredentialSelectionCard(
    disclosure: DeviceRequestCredentialDisclosure<SubjectCredentialStore.StoreEntry>,
    selection: MutableState<DeviceRequestCredentialDisclosure<SubjectCredentialStore.StoreEntry>>,
    trustListService: TrustListService,
    imageDecoder: ImageDecoder = koinInject(),
    checkCredentialFreshness: CredentialFreshnessSummaryModelEvaluator = koinInject(),
) {
    val resolvedCredential by produceState<ResolvedCredential?>(null, disclosure.credential) {
        value = catchingUnwrapped { disclosure.credential.toResolvedCredential() }
            .getOrElse { disclosure.credential.toFallbackResolvedCredential() }
    }
    val freshness by produceState(
        CredentialFreshnessValidationStateUiModel.Loading as CredentialFreshnessValidationStateUiModel,
        disclosure.credential,
    ) {
        value = CredentialFreshnessValidationStateUiModel.Done(checkCredentialFreshness(disclosure.credential))
    }
    val trustState by trustListService.observeTrustStateForEntry(flowOf(resolvedCredential))
        .collectAsState(initial = TrustState.EVALUATING)
    val displayCredential = resolvedCredential ?: return

    CredentialSelectionCardLayout(
        onClick = { selection.value = disclosure },
        isSelected = selection.value == disclosure,
        isError = (freshness as? CredentialFreshnessValidationStateUiModel.Done)
            ?.credentialFreshnessSummary?.isNotBad == false,
        modifier = Modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = freshness,
            credential = displayCredential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = false,
            matchingException = null,
        )
        TrustStatusBanner(trustState, Modifier.fillMaxWidth().padding(vertical = 8.dp))
        CredentialSummaryCardContent(displayCredential) { imageDecoder(it) }
        disclosure.disclosedAttributes.forEach { path ->
            Text(path.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}
