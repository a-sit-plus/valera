package ui.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.WalletMain
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
    val preparationState = catching {
        route.authorizationResponsePreparationState
    }

    val relyingPartyValidation = catchingUnwrapped { route.relyingPartyValidation }.getOrNull()
    val selectionProvider = MutableStateFlow<UiState<CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                UiStateSuccess(
                    walletMain.presentationService.getMatchingCredentials(
                        preparationState = route.authorizationResponsePreparationState
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
        // Ignore repeated confirmations while a presentation is being submitted
        if (!mutableIsSubmitting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val result = finalizeAuthorization(presentation)
                onSuccess(result)
            } catch (_: at.asitplus.signum.supreme.UserInitiatedCancellationReason) {
                mutableIsSubmitting.value = false
            } catch (it: Throwable) {
                mutableIsSubmitting.value = false
                onFailure(it)
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
            preparationState = route.authorizationResponsePreparationState
        )
        return authenticationResult as? OpenId4VpKtorHolder.AuthenticationSuccess
            ?: throw IllegalStateException("DC API requests must use the DC API presentation flow")
    }
}
