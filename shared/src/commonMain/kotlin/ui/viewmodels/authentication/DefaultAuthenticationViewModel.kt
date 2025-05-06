package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.dcapi.request.Oid4vpDCAPIRequest
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.QesInputDescriptor
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState


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
    val dcapiRequest: Oid4vpDCAPIRequest?
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
            ?.inputDescriptors?.filterIsInstance<QesInputDescriptor>()?.firstOrNull()?.transactionData?.firstOrNull()

    override suspend fun findMatchingCredentials(): KmmResult<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        catching {
            return walletMain.presentationService.getMatchingCredentials(
                preparationState = preparationState,
                oid4vpDCAPIRequest = dcapiRequest
            )
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) : OpenId4VpWallet.AuthenticationSuccess {
        val authenticationResult = walletMain.presentationService.finalizeAuthorizationResponse(
            request = authenticationRequest,
            clientMetadata = authenticationRequest.parameters.clientMetadata,
            credentialPresentation = credentialPresentation,
            dcApiRequest = dcapiRequest
        )
        return when (authenticationResult) {
            is OpenId4VpWallet.AuthenticationForward -> finalizeDcApi(authenticationResult)
            is OpenId4VpWallet.AuthenticationSuccess -> authenticationResult
        }
    }

    private suspend fun finalizeDcApi(
        authenticationResult: OpenId4VpWallet.AuthenticationForward,
    ): OpenId4VpWallet.AuthenticationSuccess {
        authenticationResult.authenticationResponseResult.params.response?.let {
            walletMain.presentationService.finalizeDCAPIPresentation(it)
        } ?: throw IllegalArgumentException("Not response has been generated")
        return OpenId4VpWallet.AuthenticationSuccess()
    }
}