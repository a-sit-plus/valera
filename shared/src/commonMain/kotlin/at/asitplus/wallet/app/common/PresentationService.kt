package at.asitplus.wallet.app.common

import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.PreviewRequest
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import com.benasher44.uuid.uuid4
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient

class PresentationService(
    val platformAdapter: PlatformAdapter,
    cryptoService: WalletCryptoService,
    val holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val presentationService = OpenId4VpWallet(
        openUrlExternally = { platformAdapter.openUrl(it) },
        engine = HttpClient().engine,
        httpClientConfig = httpService.loggingConfig,
        cryptoService = cryptoService,
        holderAgent = holderAgent,
    )

    suspend fun parseAuthenticationRequestParameters(requestUri: String) =
        presentationService.parseAuthenticationRequestParameters(requestUri)

    suspend fun startAuthorizationResponsePreparation(request: RequestParametersFrom<AuthenticationRequestParameters>) =
        presentationService.startAuthorizationResponsePreparation(request)

    suspend fun getPreparationState(request: RequestParametersFrom<AuthenticationRequestParameters>) =
        presentationService.startAuthorizationResponsePreparation(request).getOrThrow()

    suspend fun getMatchingCredentials(preparationState: AuthorizationResponsePreparationState) =
        holderAgent.matchInputDescriptorsAgainstCredentialStore(
            inputDescriptors = preparationState.presentationDefinition?.inputDescriptors!!,
            fallbackFormatHolder = preparationState.clientMetadata?.vpFormats,
        ).getOrThrow()

    suspend fun finalizeAuthorizationResponse(
        request: RequestParametersFrom<AuthenticationRequestParameters>,
        preparationState: AuthorizationResponsePreparationState,
        inputDescriptorSubmission: Map<String, CredentialSubmission>
    ) {
        presentationService.finalizeAuthorizationResponse(
            request = request,
            preparationState = preparationState,
            inputDescriptorSubmission = inputDescriptorSubmission
        ).getOrThrow()
    }

    suspend fun finalizeDCAPIPreviewPresentation(
        submission: Map<String, CredentialSubmission>,
        dcApiRequest: DCAPIRequest
    ) {
        Napier.d("Finalizing DCAPI response")
        val previewRequest = PreviewRequest.deserialize(dcApiRequest.request).getOrThrow()

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(nonce = previewRequest.nonce, audience = dcApiRequest.callingOrigin ?: dcApiRequest.callingPackageName!!),
            presentationDefinitionId = uuid4().toString(),
            presentationSubmissionSelection = submission
        )

        val presentation = presentationResult.getOrThrow()

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse
            is CreatePresentationResult.SdJwt -> TODO("Credential type not yet supported for API use case")
            is CreatePresentationResult.Signed -> TODO("Credential type not yet supported for API use case")
        }

        platformAdapter.prepareDCAPICredentialResponse(deviceResponse.serialize(), dcApiRequest)
    }

}