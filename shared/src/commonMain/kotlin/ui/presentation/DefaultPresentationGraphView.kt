package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import at.asitplus.wallet.lib.agent.Validator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DefaultPresentationGraphView(
    onNavigateUp: () -> Unit,
    onError: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    koinScope: Scope,
    viewModel: DefaultPresentationGraphViewModel = koinViewModel(scope = koinScope),
    credentialValidator: Validator = koinInject(),
) {
    val dcApiRequest = try {
        viewModel.dcApiRequest.getOrThrow()
    } catch (throwable: Throwable) {
        onError(throwable)
        return
    }

    val presentationRequest = try {
        viewModel.preparationState.getOrThrow().credentialPresentationRequest
    } catch (throwable: Throwable) {
        onError(throwable)
        return
    }

    val spLocation = dcApiRequest?.callingOrigin ?: viewModel.route.recipientLocation
    val spName = dcApiRequest?.callingPackageName

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    val selectionProvider by viewModel.selectionProvider.collectAsState()

    PresentationGraphView(
        serviceProviderLogo = null,
        serviceProviderNameLocalized = spName,
        serviceProviderLocationLocalized = spLocation,
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        onNavigateUp = onNavigateUp,
        onError = onError,
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        selectionProvider = selectionProvider,
        presentationRequest = presentationRequest,
        submitPresentation = SubmitPresentation { it, navigate ->
            viewModel.confirmSelection(
                credentialPresentationSubmissions = it,
                onFailure = onError,
                onSuccess = {
                    navigate(
                        PresentationSuccessRoute(
                            redirectUrl = it.redirectUri,
                            isCrossDeviceFlow = viewModel.route.isCrossDeviceFlow,
                        )
                    )
                }
            )
        },
        transactionData = try {
            viewModel.route.authenticationRequest.parameters.transactionData?.firstOrNull()
        } catch (throwable: Throwable) {
            LaunchedEffect(Unit) {
                onError(throwable)
            }
            null
        }
    )
}



