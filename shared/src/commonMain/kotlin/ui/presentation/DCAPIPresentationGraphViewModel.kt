package ui.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import at.asitplus.catching
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.dif.PresentationDefinition
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.toDifInputDescriptorList
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.DCAPIAuthenticationConsentRoute
import ui.viewmodels.authentication.CredentialPresentationSubmissions
import ui.viewmodels.authentication.DCQLCredentialSubmissions
import ui.viewmodels.authentication.PresentationExchangeCredentialSubmissions

class DCAPIPresentationGraphViewModel(
    savedStateHandle: SavedStateHandle,
    private val walletMain: WalletMain,
) : ViewModel() {
    val route = savedStateHandle.toRoute<DCAPIAuthenticationConsentRoute>()

    val apiRequestSerialized = route.apiRequestSerialized

    val dcApiWalletRequest = catching {
        vckJsonSerializer.decodeFromString<DCAPIWalletRequest.IsoMdoc>(apiRequestSerialized)
    }

    val selectionProvider = MutableStateFlow<UiState<Pair<DCAPIWalletRequest.IsoMdoc, CredentialSelectionProvider<SubjectCredentialStore.StoreEntry>>>>(
        UiStateLoading
    ).apply {
        viewModelScope.launch {
            value = try {
                val unwrappedDcApiWalletRequest = dcApiWalletRequest.getOrThrow()
                val descriptors = unwrappedDcApiWalletRequest.isoMdocRequest.deviceRequest.docRequests.toDifInputDescriptorList()
                val presentationRequest = CredentialPresentationRequest.PresentationExchangeRequest(
                    presentationDefinition = PresentationDefinition(
                        inputDescriptors = descriptors
                    )
                )
                UiStateSuccess(
                    unwrappedDcApiWalletRequest to PresentationExchangeMatchingResult(
                        presentationRequest = CredentialPresentationRequest.PresentationExchangeRequest(
                            presentationDefinition = PresentationDefinition(
                                inputDescriptors = presentationRequest.presentationDefinition.inputDescriptors,
                            )
                        ),
                        matchingResult = walletMain.holderAgent.matchInputDescriptorsAgainstCredentialStoreV2(
                            inputDescriptors = presentationRequest.presentationDefinition.inputDescriptors,
                            fallbackFormatHolder = null,
                        ).getOrThrow(),
                    ).toCredentialSelectionProvider(viewModelScope) {
                        walletMain.checkCredentialFreshness(it)
                    }
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
        val (request, matchingResult) = (selectionProvider.value as? UiStateSuccess)?.value ?: return
        val presentationRequest = matchingResult.queryMatchingResult.presentationRequest
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
                val result = finalizeAuthorization(
                    credentialPresentation = presentation,
                    request = request,
                )
                onSuccess(result)
            } catch (it: Throwable) {
                onFailure(it)
            }
        }
    }

    private suspend fun finalizeAuthorization(
        credentialPresentation: CredentialPresentation,
        request: DCAPIWalletRequest.IsoMdoc
    ): OpenId4VpWallet.AuthenticationSuccess {
        walletMain.keyMaterial.promptText =
            getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
        return finalizationMethod(
            credentialPresentation,
            request = request,
        )
    }

    private suspend fun finalizationMethod(
        credentialPresentation: CredentialPresentation,
        request: DCAPIWalletRequest.IsoMdoc
    ): OpenId4VpWallet.AuthenticationSuccess = walletMain.presentationService.finalizeIsoMdocDCAPIPresentation(
        credentialPresentation = when (credentialPresentation) {
            is CredentialPresentation.PresentationExchangePresentation -> credentialPresentation
            else -> throw IllegalArgumentException()
        },
        isoMdocWalletRequest = request,
    )
}