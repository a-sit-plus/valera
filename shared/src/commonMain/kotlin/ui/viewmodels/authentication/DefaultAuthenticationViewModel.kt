package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
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

    override suspend fun findMatchingCredentials(): Result<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        walletMain.presentationService.getMatchingCredentials(
            preparationState = preparationState
        )

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation): OpenId4VpWallet.AuthenticationSuccess {
        val authenticationResult = walletMain.presentationService.finalizeAuthorizationResponse(
            credentialPresentation = credentialPresentation,
            preparationState = preparationState
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
