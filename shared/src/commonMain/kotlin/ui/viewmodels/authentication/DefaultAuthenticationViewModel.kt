package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dcapi.DCAPIResponse
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.OpenIdConstants
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.QesInputDescriptor
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import at.asitplus.wallet.lib.openid.CredentialMatchingResult


class DefaultAuthenticationViewModel(
    spName: String?,
    spLocation: String,
    spImage: ImageBitmap?,
    val authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
    val preparationState: AuthorizationResponsePreparationState,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: (redirectUrl: String?) -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
) : AuthenticationViewModel(
    spName,
    spLocation,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain,
    onClickLogo,
    onClickSettings
) {
    override val presentationRequest: CredentialPresentationRequest
        get() = preparationState.credentialPresentationRequest
            ?: throw IllegalArgumentException("No credential presentation request has been found.")

    @Suppress("DEPRECATION")
    override val transactionData = authenticationRequest.parameters.transactionData?.firstOrNull()
        ?: authenticationRequest.parameters.presentationDefinition
            ?.inputDescriptors?.filterIsInstance<QesInputDescriptor>()
            ?.firstOrNull()?.transactionData?.firstOrNull()

    override suspend fun findMatchingCredentials(): Result<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        walletMain.presentationService.getMatchingCredentials(
            preparationState = preparationState,
            request = authenticationRequest
        )

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpWallet.AuthenticationSuccess {
        val authenticationResult = walletMain.presentationService.finalizeAuthorizationResponse(
            request = authenticationRequest,
            clientMetadata = authenticationRequest.parameters.clientMetadata,
            credentialPresentation = credentialPresentation,
        )
        return when (authenticationResult) {
            is OpenId4VpWallet.AuthenticationForward -> {
                val isEncryptedResponse =
                    authenticationRequest.parameters.responseMode == OpenIdConstants.ResponseMode.DcApiJwt
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
