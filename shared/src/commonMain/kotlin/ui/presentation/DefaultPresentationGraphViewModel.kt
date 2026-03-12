package ui.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.dcapi.request.DCAPIWalletRequest
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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.AuthenticationViewRoute
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.PresentationExchangeCredentialSubmissions

class DefaultPresentationGraphViewModel(
    savedStateHandle: SavedStateHandle,
    private val walletMain: WalletMain,
) : ViewModel() {
    val route = savedStateHandle.toRoute<AuthenticationViewRoute>()
    val preparationState = catching {
        route.authorizationResponsePreparationState
    }

    val dcApiRequest = preparationState.map {
        when (val request = it.request) {
            is RequestParametersFrom.DcApiSigned -> request.dcApiRequest
            is RequestParametersFrom.DcApiUnsigned -> request.dcApiRequest
            else -> null
        } as? DCAPIWalletRequest
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
        onSuccess: (OpenId4VpWallet.AuthenticationSuccess) -> Unit,
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
            } catch (_: at.asitplus.signum.supreme.UserInitiatedCancellationReason) {
                // ignore
            } catch (it: Throwable) {
                onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation
    ): OpenId4VpWallet.AuthenticationSuccess {
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
            is OpenId4VpWallet.AuthenticationForward -> finalizeDcApi(authenticationResult)
            is OpenId4VpWallet.AuthenticationSuccess -> authenticationResult
        }
    }

    private fun finalizeDcApi(
        authenticationResult: OpenId4VpWallet.AuthenticationForward,
    ): OpenId4VpWallet.AuthenticationSuccess {
        authenticationResult.authenticationResponseResult.params.let {
            val serializedResponse = vckJsonSerializer.encodeToString(it)
            walletMain.presentationService.finalizeOid4vpDCAPIPresentation(serializedResponse)
        }
        return OpenId4VpWallet.AuthenticationSuccess()
    }
}



