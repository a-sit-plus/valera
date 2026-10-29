package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun DefaultPresentationGraphView(
    onNavigateUp: () -> Unit,
    errorAction: (Throwable) -> Unit,
    onClickLogo: () -> Unit,
    koinScope: Scope,
    navigateUpIsClose: Boolean = false,
    viewModel: DefaultPresentationGraphViewModel = koinViewModel(scope = koinScope),
) {
    val presentationRequest = try {
        viewModel.preparationState.getOrThrow().credentialPresentationRequest
    } catch (throwable: Throwable) {
        errorAction(throwable)
        return
    }

    val spLocation = viewModel.route.recipientLocation
    val spName: String? = null

    val authenticateAtRelyingParty = spLocation != "Local Presentation"

    val selectionProvider by viewModel.selectionProvider.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    PresentationGraphView(
        koinScope = koinScope,
        serviceProviderLogo = null,
        serviceProviderNameLocalized = spName,
        serviceProviderLocationLocalized = spLocation,
        authenticateAtRelyingParty = authenticateAtRelyingParty,
        onNavigateUp = onNavigateUp,
        onError = errorAction,
        onClickLogo = onClickLogo,
        selectionProvider = selectionProvider,
        presentationRequest = presentationRequest,
        navigateUpIsClose = navigateUpIsClose,
        isSubmitting = isSubmitting,
        submitPresentation = SubmitPresentation { it, navigate ->
            viewModel.confirmSelection(
                credentialPresentationSubmissions = it,
                onFailure = errorAction,
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
            viewModel.preparationState.getOrThrow().request.parameters.transactionData?.firstOrNull()
        } catch (throwable: Throwable) {
            LaunchedEffect(Unit) {
                errorAction(throwable)
            }
            null
        },
        trustListService = viewModel.trustListService,
        request = viewModel.preparationState.getOrThrow().request,
        relyingPartyValidation = viewModel.relyingPartyValidation,
    )
}


