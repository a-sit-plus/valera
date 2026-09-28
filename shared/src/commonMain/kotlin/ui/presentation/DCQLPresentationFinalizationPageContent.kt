package ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_submit
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_show_data_third_party
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.wallet.app.common.RelyingPartyTrustResult
import at.asitplus.wallet.app.common.TrustListService
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import ui.composables.PresentationRequestLoadingIndicator
import ui.composables.RelyingPartyDataDisplaySection
import ui.composables.ScreenHeading

@Composable
fun DCQLPresentationFinalizationPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    dcqlQuery: DCQLQuery,
    selections: Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>>,
    onError: (Throwable) -> Unit,
    onAbort: () -> Unit,
    onSubmit: () -> Unit,
    serviceProviderLogo: ImageBitmap? = null,
    trustListService: TrustListService,
    request: RequestParametersFrom<*>
) {
    val cards = remember(selections) {
        selections.entries.sortedBy { it.key.string }.flatMap { it.value }
    }
    val loadingCards = remember(cards) {
        mutableStateMapOf<SelectableCredentialSubmissionCard, Boolean>().apply {
            cards.forEach { put(it, true) }
        }
    }
    var hasLoadingError by remember(cards) { mutableStateOf(false) }
    val isLoading = loadingCards.values.any { it }

    PresentationFinalizationPageContent(
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        serviceProviderLocalizedName = serviceProviderLocalizedName,
        serviceProviderLocalizedLocation = serviceProviderLocalizedLocation,
        onAbort = onAbort,
        onSubmit = onSubmit,
        isContinueEnabled = !isLoading && !hasLoadingError,
        serviceProviderLogo = serviceProviderLogo,
        trustListService = trustListService,
        request = request
    ) {
        if (isLoading) {
            PresentationRequestLoadingIndicator()
        }
        cards.forEach { card ->
            Spacer(modifier = Modifier.height(8.dp))
            // TODO: good enough or should we have separate cards for final submissions?
            //  - if these cards should be reused, then allowMultiSelection shouldn't be relevant with (isSelected, onToggleSelection) = (true, null)
            //  - Cards should therefore implicitly handle the case (true, *, null) to show the card without any selection specific semantics UI
            card(
                isSelected = true,
                allowMultiSelection = false,
                onToggleSelection = null,
                onLoadingChanged = { loadingCards[card] = it },
                onError = {
                    hasLoadingError = true
                    onError(it)
                },
            )
        }
    }
}

@Composable
fun PresentationFinalizationPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    onAbort: () -> Unit,
    onSubmit: () -> Unit,
    isContinueEnabled: Boolean = true,
    serviceProviderLogo: ImageBitmap? = null,
    trustListService: TrustListService,
    request: RequestParametersFrom<*>,
    content: @Composable ColumnScope.() -> Unit,
) {
    val relyingPartyTrust by trustListService
        .observeRelyingPartyTrust(flowOf(request))
        .collectAsState(initial = RelyingPartyTrustResult.Evaluating)
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_send_above_data),
                onAbort = onAbort,
                onContinue = onSubmit.takeIf { isContinueEnabled },
                useBackButton = true,
                continueButtonLabel = Res.string.button_label_submit,
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it).fillMaxSize(),
        ) {
            val title = if (authenticateAtRelyingParty) {
                stringResource(Res.string.heading_label_authenticate_at_device_screen)
            } else {
                stringResource(Res.string.heading_label_show_data_third_party)
            }
            ScreenHeading(title, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))

            Column(
                modifier = Modifier.verticalScroll(state = rememberScrollState()).padding(16.dp),
            ) {
                if (serviceProviderLogo != null) {
                    Box(Modifier.Companion.fillMaxWidth(), contentAlignment = Alignment.Companion.Center) {
                        Image(
                            bitmap = serviceProviderLogo,
                            contentDescription = null,
                            contentScale = ContentScale.Companion.Fit,
                            modifier = Modifier.Companion.height(64.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                RelyingPartyDataDisplaySection(
                    serviceProviderName = serviceProviderLocalizedName,
                    serviceProviderLocation = serviceProviderLocalizedLocation,
                    trustResult = relyingPartyTrust,
                )

                content()
            }
        }
    }
}
