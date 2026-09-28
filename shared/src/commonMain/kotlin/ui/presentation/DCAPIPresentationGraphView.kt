package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.lib.agent.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun DCAPIPresentationGraphView(
    onNavigateUp: () -> Unit,
    errorAction: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    koinScope: Scope,
    showStartRoute: Boolean = true,
    viewModel: DCAPIPresentationGraphViewModel = koinViewModel(scope = koinScope),
) {
    val dcApiRequest = try {
        viewModel.dcApiWalletRequest.getOrThrow()
    } catch (it: Throwable) {
        return errorAction(it)
    }

    val spName = dcApiRequest.callingPackageName
    val spLocation = dcApiRequest.callingOrigin

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    val matchingResult by viewModel.selectionProvider.collectAsState()
    val preparedState = (matchingResult as? UiStateSuccess)?.value
    val queryMatchingResult = preparedState?.selectionProvider?.queryMatchingResult
    val verifierSignatures = (preparedState?.preparationState as? DcApiPreparationState.OpenId4Vp)
        ?.state?.verifierSignatures
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
        onError = errorAction,
        onClickLogo = onClickLogo,
        navigateUpIsClose = true,
        selectionProvider = matchingResult.map {
            it.selectionProvider
        },
        submitPresentation = { it, navigate ->
            viewModel.confirmSelection(
                credentialPresentationSubmissions = it,
                onFailure = errorAction,
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
        trustListService = viewModel.trustListService,
        request = dcApiRequest,
        wrpValidationResult = viewModel.wrpValidationResult,
        verifierSignatures = verifierSignatures,
    )
}
