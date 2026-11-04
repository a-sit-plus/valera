package ui.presentation

import ErrorHandlingOverrideException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.warning_authentication_cancelled
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.app.common.validation.integrityRejection
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val credentialQuerySelections = catching { route.credentialQuerySelections }

    val trustListService = walletMain.trustListService

    val selectionProvider = MutableStateFlow<UiState<DcApiPresentationUiState>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                val unwrappedDcApiWalletRequest = dcApiWalletRequest.getOrThrow()
                // Validate against fresh Trust Lists if they arrive in time, e.g. in the short-lived iOS extension
                trustListService.refreshStaleTrustLists(Configuration.TRUST_LIST_REFRESH_TIMEOUT)
                val validation = walletMain.presentationService.prepareDcApiRequest(
                    unwrappedDcApiWalletRequest
                ).getOrThrow()
                val relyingPartyValidation = RelyingPartyValidationUi.of(
                    report = validation.validationReport,
                    relyingParty = validation.relyingParty,
                )
                // The user is warned about the relying party, but a request that failed its integrity is rejected
                if (relyingPartyValidation.outcome?.integrityFailed == true) {
                    throw validation.validationReport?.integrityRejection()
                        ?: OAuth2Exception.InvalidRequest("The request has been rejected")
                }
                val preparationState = validation.preparationState
                val matchingResult = walletMain.presentationService.getMatchingCredentials(
                    preparationState
                ).getOrThrow()
                UiStateSuccess(
                    DcApiPresentationUiState(
                        preparationState = preparationState,
                        selectionProvider = matchingResult.toCredentialSelectionProvider(viewModelScope) {
                            walletMain.checkCredentialFreshness(it)
                        },
                        relyingPartyValidation = relyingPartyValidation,
                    )
                )
            } catch (it: Throwable) {
                // Rethrows the cancellation of the flow, but not one of e.g. a request whose job was cancelled, which
                // would leave the request loading forever
                ensureActive()
                UiStateError(withErrorResponse(it))
            }
        }
    }

    /**
     * Sends [error] to the caller of the DC API once the user acknowledges it, as the flow ends without a
     * presentation, unless [error] already sends a response on acknowledgement.
     */
    fun withErrorResponse(error: Throwable): Throwable {
        val override = error as? ErrorHandlingOverrideException
        if (override?.onAcknowledge != null) return error
        val cause = override?.cause ?: error
        val response = when (cause) {
            is OAuth2Exception -> cause
            else -> OAuth2Exception.InvalidRequest(cause.message)
        }.serialize()
        return ErrorHandlingOverrideException(
            resetStackOverride = override?.resetStackOverride,
            actionDescriptionOverride = override?.actionDescriptionOverride,
            onAcknowledge = {
                walletMain.platformAdapter.prepareDCAPICredentialError(response)
            },
            cause = cause,
        )
    }

    private val mutableIsSubmitting = MutableStateFlow(false)

    /** Whether a presentation is being submitted, which stays set after success until the flow is left */
    val isSubmitting: StateFlow<Boolean> = mutableIsSubmitting.asStateFlow()

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
        // Ignore repeated confirmations while a presentation is being submitted
        if (!mutableIsSubmitting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                finalizeAuthorization(
                    credentialPresentation = presentation,
                    preparationState = uiState.preparationState,
                )
                onSuccess()
            } catch (_: UserInitiatedCancellationReason) {
                mutableIsSubmitting.value = false
                walletMain.snackbarService.showSnackbar(
                    getString(Res.string.warning_authentication_cancelled)
                )
            } catch (it: Throwable) {
                // The flow has been left if it is cancelled, there is no one to report a failure to
                ensureActive()
                mutableIsSubmitting.value = false
                onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation,
        preparationState: DcApiPreparationState,
    ) {
        walletMain.presentationService.finalizeDcApiPresentation(
            credentialPresentation = credentialPresentation,
            preparationState = preparationState,
        )
    }
}

data class DcApiPresentationUiState(
    val preparationState: DcApiPreparationState,
    val selectionProvider: CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>,
    val relyingPartyValidation: RelyingPartyValidationUi,
)
