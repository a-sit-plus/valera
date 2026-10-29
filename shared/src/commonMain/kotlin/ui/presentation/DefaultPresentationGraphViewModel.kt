package ui.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyStatusResolution
import at.asitplus.wallet.app.common.relyingParty.RelyingPartyValidationUi
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpKtorHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ui.navigation.routes.AuthenticationViewRoute
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.IsoDeviceRequestCredentialSubmissions

class DefaultPresentationGraphViewModel(
    savedStateHandle: SavedStateHandle,
    private val walletMain: WalletMain,
) : ViewModel() {
    val route = savedStateHandle.toRoute<AuthenticationViewRoute>()
    val trustListService = walletMain.trustListService
    private val validatedRequests = walletMain.presentationService.validatedRequests

    /** The request of [route], which is gone if the session is not the one that validated it, e.g. after process death */
    private val request = catching {
        validatedRequests[route.requestId]
            ?: throw IllegalStateException("The request is no longer available, open it again")
    }
    val preparationState = request.map { it.preparationState }

    private val relyingParty = request.getOrNull()?.relyingParty ?: RelyingPartyStatusResolution.resolved(null)

    /** The validation of the relying party, replaced once the status of its registration certificates arrives */
    val relyingPartyValidation: StateFlow<RelyingPartyValidationUi?> = relyingParty.validation
    val selectionProvider = MutableStateFlow<UiState<CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                UiStateSuccess(
                    walletMain.presentationService.getMatchingCredentials(
                        preparationState = preparationState.getOrThrow()
                    ).getOrThrow().toCredentialSelectionProvider(
                        scope = viewModelScope
                    ) {
                        walletMain.checkCredentialFreshness(it)
                    }
                )
            } catch (it: Throwable) {
                UiStateError(it)
            }
        }
    }

    private val mutableIsSubmitting = MutableStateFlow(false)

    /** Whether a presentation is being submitted, which stays set after success until the flow is left */
    val isSubmitting: StateFlow<Boolean> = mutableIsSubmitting.asStateFlow()

    private val gate = RelyingPartyStatusGate(MutableStateFlow(relyingParty), ::submit)

    /** Holds back a presentation confirmed while the status of the relying party is pending */
    val statusGate: RelyingPartyStatusGate<*>
        get() = gate

    private class Submission(
        val presentation: CredentialPresentation,
        val onSuccess: (OpenId4VpKtorHolder.AuthenticationSuccess) -> Unit,
        val onFailure: (Throwable) -> Unit,
    )

    fun confirmSelection(
        credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>,
        onSuccess: (OpenId4VpKtorHolder.AuthenticationSuccess) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val selectionProvider = selectionProvider.value as? UiStateSuccess ?: return
        val presentationRequest = selectionProvider.value.queryMatchingResult.presentationRequest
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
        gate.confirm(Submission(presentation, onSuccess, onFailure))
    }

    private fun submit(submission: Submission) {
        // Ignore repeated confirmations while a presentation is being submitted
        if (!mutableIsSubmitting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val result = finalizeAuthorization(submission.presentation)
                submission.onSuccess(result)
            } catch (_: at.asitplus.signum.supreme.UserInitiatedCancellationReason) {
                mutableIsSubmitting.value = false
            } catch (it: Throwable) {
                mutableIsSubmitting.value = false
                submission.onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation
    ): OpenId4VpKtorHolder.AuthenticationSuccess {
        return finalizationMethod(credentialPresentation)
    }

    private suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpKtorHolder.AuthenticationSuccess {
        val authenticationResult = walletMain.presentationService.finalizeAuthorizationResponse(
            credentialPresentation = credentialPresentation,
            preparationState = preparationState.getOrThrow()
        )
        return authenticationResult as? OpenId4VpKtorHolder.AuthenticationSuccess
            ?: throw IllegalStateException("DC API requests must use the DC API presentation flow")
    }

    /** The consent screen is left for good, so its request is not needed any more. */
    override fun onCleared() {
        validatedRequests.remove(route.requestId)
    }
}
