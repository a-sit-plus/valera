package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DCAPIPresentationGraphView(
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    koinScope: Scope,
    viewModel: DCAPIPresentationGraphViewModel = koinViewModel(scope = koinScope),
) {
    val isoMdocRequest = try {
        viewModel.dcApiWalletRequest.getOrThrow()
    } catch (it: Throwable) {
        return onError(it)
    }

    val spName = isoMdocRequest.callingPackageName
    val spLocation = isoMdocRequest.callingOrigin

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    val matchingResult by viewModel.matchingResult.collectAsState()
    PresentationGraphView(
        serviceProviderLogo = null,
        serviceProviderNameLocalized = spName,
        serviceProviderLocationLocalized = spLocation,
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        onNavigateUp = onNavigateUp,
        onError = onError,
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        matchingResult = matchingResult.map {
            it.second
        },
        submitPresentation = SubmitPresentation { it, navigate ->
            viewModel.confirmSelection(
                credentialPresentationSubmissions = it,
                onFailure = onError,
                onSuccess = {
                    navigate(
                        PresentationSuccessRoute(
                            redirectUrl = it.redirectUri,
                            isCrossDeviceFlow = false,
                        )
                    )
                }
            )
        },
        transactionData = null,
        presentationRequest = null
    )
}