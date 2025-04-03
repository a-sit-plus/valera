package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.rqes.QesInputDescriptor
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState


class DefaultAuthenticationViewModel(
    spName: String?,
    spLocation: String,
    spImage: ImageBitmap?,
    val authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
    val isCrossDeviceFlow: Boolean,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    onClickLogo: () -> Unit
) : AuthenticationViewModel(
    spName,
    spLocation,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain,
    onClickLogo
) {
    override val presentationRequest: CredentialPresentationRequest
        get() = authenticationRequest.parameters.presentationDefinition?.let {
            CredentialPresentationRequest.PresentationExchangeRequest(
                presentationDefinition = it,
                fallbackFormatHolder = authenticationRequest.parameters.clientMetadata?.vpFormats,
            )
        } ?: authenticationRequest.parameters.dcqlQuery?.let {
            CredentialPresentationRequest.DCQLRequest(it)
        } ?: throw IllegalArgumentException("No credential presentation request has been found.")

    @Suppress("DEPRECATION")
    override val transactionData = authenticationRequest.parameters.transactionData?.firstOrNull()
        ?: authenticationRequest.parameters.presentationDefinition
            ?.inputDescriptors?.filterIsInstance<QesInputDescriptor>()?.firstOrNull()?.transactionData?.firstOrNull()

    private lateinit var preparationState: AuthorizationResponsePreparationState

    override suspend fun findMatchingCredentials(): KmmResult<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        catching {
            preparationState = walletMain.presentationService.getPreparationState(request = authenticationRequest)

            return walletMain.presentationService.getMatchingCredentials(preparationState = preparationState)
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) =
        walletMain.presentationService.finalizeAuthorizationResponse(
            request = authenticationRequest,
            clientMetadata = authenticationRequest.parameters.clientMetadata,
            credentialPresentation = credentialPresentation,
            isCrossDeviceFlow = isCrossDeviceFlow,
        )
}