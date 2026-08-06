package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.lib.agent.DCQLMatchingResult
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope

@ExperimentalMaterial3Api
@Composable
fun DCAPIPresentationGraphView(
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    koinScope: Scope,
    showStartRoute: Boolean = true,
    viewModel: DCAPIPresentationGraphViewModel = koinViewModel(scope = koinScope),
) {
    val dcApiRequest = try {
        viewModel.dcApiWalletRequest.getOrThrow()
    } catch (it: Throwable) {
        return onError(it)
    }

    val spName = dcApiRequest.callingPackageName
    val spLocation = dcApiRequest.callingOrigin

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    val matchingResult by viewModel.selectionProvider.collectAsState()
    val queryMatchingResult = (matchingResult as? UiStateSuccess)?.value
        ?.selectionProvider?.queryMatchingResult
    val selectedCredentialQueryIds = if (dcApiRequest.credentialIds?.isNotEmpty() == true) {
        (queryMatchingResult as? DCQLMatchingResult<*>)
            ?.matchingResult?.credentialQueryMatches
            ?.filterValues { it.isNotEmpty() }
            ?.keys
            .orEmpty()
    } else {
        emptySet()
    }
    PresentationGraphView(
        koinScope = koinScope,
        serviceProviderLogo = null,
        serviceProviderNameLocalized = spName,
        serviceProviderLocationLocalized = spLocation,
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        onNavigateUp = onNavigateUp,
        onError = onError,
        onClickLogo = onClickLogo,
        navigateUpIsClose = true,
        selectionProvider = matchingResult.map {
            it.selectionProvider
        },
        submitPresentation = { it, navigate ->
            viewModel.confirmSelection(
                credentialPresentationSubmissions = it,
                onFailure = onError,
                onSuccess = {
                    navigate(
                        PresentationSuccessRoute(
                            redirectUrl = null,
                            isCrossDeviceFlow = false,
                        )
                    )
                }
            )
        },
        transactionData = when (dcApiRequest) {
            is RequestParametersFrom.OpenId4VpDcApiUnsigned,
            is RequestParametersFrom.OpenId4VpDcApiSigned,
            is RequestParametersFrom.OpenId4VpDcApiMultiSigned -> dcApiRequest.parameters.transactionData?.firstOrNull()

            is RequestParametersFrom.IsoMdocDcApi -> null
        },
        presentationRequest = queryMatchingResult?.presentationRequest,
        credentialQueryIdsSelectedForPresentation = selectedCredentialQueryIds,
        showStartRoute = showStartRoute,
        fixedCredentialSelection = dcApiRequest.credentialIds?.isNotEmpty() == true,
        trustListService = viewModel.trustListService
    )
}
