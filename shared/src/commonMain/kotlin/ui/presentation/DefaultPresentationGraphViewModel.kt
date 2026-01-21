package ui.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.dcapi.DCAPIResponse
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.DefaultPresentationGraphRoute
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.PresentationExchangeCredentialSubmissions

// TODO: what to do about abstract finalizationMethod?
class DefaultPresentationGraphViewModel(
    savedStateHandle: SavedStateHandle,
    private val walletMain: WalletMain,
) : ViewModel() {
    val route = savedStateHandle.toRoute<DefaultPresentationGraphRoute>()

    val dcApiRequest = catching {
        when (val request = route.authorizationResponsePreparationState.request) {
            is RequestParametersFrom.DcApiSigned<*> -> request.dcApiRequest
            is RequestParametersFrom.DcApiUnsigned<*> -> request.dcApiRequest
            else -> null
        }
    }

    val matchingResult = MutableStateFlow<UiState<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                UiStateSuccess(
                    walletMain.presentationService.getMatchingCredentials(
                        preparationState = route.authorizationResponsePreparationState
                    ).getOrThrow()
                )
            } catch (it: Throwable) {
                UiStateError(it)
            }
        }
    }

    fun confirmSelection(
        credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>,
        onSuccess: (OpenId4VpWallet.AuthenticationResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val matchingResult = matchingResult.value as? UiStateSuccess ?: return
        val presentationRequest = matchingResult.value.presentationRequest
        val presentation = try {
            when (credentialPresentationSubmissions) {
                is DCQLCredentialSubmissions -> CredentialPresentation.DCQLPresentation(
                    presentationRequest = presentationRequest as CredentialPresentationRequest.DCQLRequest,
                    credentialQuerySubmissions = credentialPresentationSubmissions.credentialQuerySubmissions
                )

                is PresentationExchangeCredentialSubmissions -> CredentialPresentation.PresentationExchangePresentation(
                    presentationRequest = presentationRequest as CredentialPresentationRequest.PresentationExchangeRequest,
                    inputDescriptorSubmissions = credentialPresentationSubmissions.inputDescriptorSubmissions
                )
            }
        } catch (it: Throwable) {
            return onFailure(it)
        }
        viewModelScope.launch {
            try {
                val result = finalizeAuthorization(presentation)
                onSuccess(result)
            } catch (it: Throwable) {
                onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation
    ): OpenId4VpWallet.AuthenticationResult {
        walletMain.keyMaterial.promptText =
            getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
        return finalizationMethod(credentialPresentation)
    }

    private suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpWallet.AuthenticationSuccess {
        val authenticationResult = walletMain.presentationService.finalizeAuthorizationResponse(
            credentialPresentation = credentialPresentation,
            preparationState = route.authorizationResponsePreparationState
        )
        return when (authenticationResult) {
            is OpenId4VpWallet.AuthenticationForward -> {
                val isEncryptedResponse =
                    route.authenticationRequest.parameters.responseMode == OpenIdConstants.ResponseMode.DcApiJwt
                finalizeDcApi(authenticationResult, isEncryptedResponse)
            }

            is OpenId4VpWallet.AuthenticationSuccess -> authenticationResult
        }
    }

    private fun finalizeDcApi(
        authenticationResult: OpenId4VpWallet.AuthenticationForward,
        isEncryptedResponse: Boolean,
    ): OpenId4VpWallet.AuthenticationSuccess {
        authenticationResult.authenticationResponseResult.params.response?.let {
            // TODO no response json required for non-encrypted case?
            // at least https://digital-credentials.dev/ only works without it
            val response = if (isEncryptedResponse)
                vckJsonSerializer.encodeToString(DCAPIResponse.createOid4vpResponse(it))
            else it
            walletMain.presentationService.finalizeOid4vpDCAPIPresentation(response)
        } ?: throw IllegalArgumentException("Not response has been generated")
        return OpenId4VpWallet.AuthenticationSuccess()
    }
}


