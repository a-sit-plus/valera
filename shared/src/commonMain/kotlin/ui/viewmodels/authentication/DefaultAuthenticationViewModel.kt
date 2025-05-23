package ui.viewmodels.authentication

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.QesInputDescriptor
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState


class DefaultAuthenticationViewModel(
    val authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
    val preparationState: AuthorizationResponsePreparationState,
    walletMain: WalletMain
) : AuthenticationViewModel(
    walletMain
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
            return walletMain.presentationService.getMatchingCredentials(preparationState = preparationState)
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) =
        walletMain.presentationService.finalizeAuthorizationResponse(
            request = authenticationRequest,
            clientMetadata = authenticationRequest.parameters.clientMetadata,
            credentialPresentation = credentialPresentation,
        )
}