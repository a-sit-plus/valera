package ui.presentation

import ErrorHandlingOverrideException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.valera.resources.warning_authentication_cancelled
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.DCAPIPresentationViewRoute
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.IsoDeviceRequestCredentialSubmissions

class DCAPIPresentationGraphViewModel(
    savedStateHandle: SavedStateHandle,
    private val walletMain: WalletMain,
) : ViewModel() {
    val route = savedStateHandle.toRoute<DCAPIPresentationViewRoute>()

    val dcApiWalletRequest = catching { route.request }

    val trustListService = walletMain.trustListService

    val selectionProvider = MutableStateFlow<UiState<DcApiPresentationUiState>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                val unwrappedDcApiWalletRequest = dcApiWalletRequest.getOrThrow()
                val preparationState = walletMain.presentationService.prepareDcApiRequest(
                    unwrappedDcApiWalletRequest
                ).getOrThrow()
                val matchingResult = walletMain.presentationService.getMatchingCredentials(
                    preparationState
                ).getOrThrow()
                UiStateSuccess(
                    DcApiPresentationUiState(
                        preparationState = preparationState,
                        selectionProvider = matchingResult.toCredentialSelectionProvider(viewModelScope) {
                            walletMain.checkCredentialFreshness(it)
                        },
                    )
                )
            } catch (it: Throwable) {
                val response = when (it) {
                    is OAuth2Exception -> it
                    else -> OAuth2Exception.InvalidRequest(it.message)
                }.serialize()
                UiStateError(
                    ErrorHandlingOverrideException(
                        onAcknowledge = {
                            walletMain.platformAdapter.prepareDCAPICredentialError(response)
                        },
                        cause = it,
                    )
                )
            }
        }
    }

    fun confirmSelection(
        credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val uiState = (selectionProvider.value as? UiStateSuccess)?.value ?: return
        val presentationRequest = uiState.selectionProvider.queryMatchingResult.presentationRequest
        val presentation = try {
            when (credentialPresentationSubmissions) {
                is DCQLCredentialSubmissions -> CredentialPresentation.DCQLPresentation(
                    presentationRequest = presentationRequest as CredentialPresentationRequest.DCQLRequest,
                    credentialQuerySubmissions = credentialPresentationSubmissions.credentialQuerySubmissions
                )

                is IsoDeviceRequestCredentialSubmissions -> CredentialPresentation.IsoDeviceRetrievalPresentation(
                    presentationRequest = presentationRequest as CredentialPresentationRequest.IsoDeviceRetrieval,
                    submissions = credentialPresentationSubmissions.submissions,
                )
            }
        } catch (it: Throwable) {
            return onFailure(it)
        }
        viewModelScope.launch {
            try {
                finalizeAuthorization(
                    credentialPresentation = presentation,
                    preparationState = uiState.preparationState,
                )
                onSuccess()
            } catch (_: UserInitiatedCancellationReason) {
                walletMain.snackbarService.showSnackbar(
                    getString(Res.string.warning_authentication_cancelled)
                )
            } catch (it: Throwable) {
                onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation,
        preparationState: DcApiPreparationState,
    ) {
        walletMain.keyMaterial.promptText =
            getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
        walletMain.presentationService.finalizeDcApiPresentation(
            credentialPresentation = credentialPresentation,
            preparationState = preparationState,
        )
    }
}

data class DcApiPresentationUiState(
    val preparationState: DcApiPreparationState,
    val selectionProvider: CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>,
)
