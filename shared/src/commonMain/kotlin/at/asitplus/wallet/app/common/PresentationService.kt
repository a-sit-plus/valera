package at.asitplus.wallet.app.common

import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RelyingPartyMetadata
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.PreviewRequest
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationException
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.cbor.CoseService
import at.asitplus.wallet.lib.iso.DeviceResponse
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.builtins.ByteArraySerializer
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult

class PresentationService(
    val platformAdapter: PlatformAdapter,
    cryptoService: WalletCryptoService,
    val holderAgent: HolderAgent,
    httpService: HttpService,
    private val coseService: CoseService
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

    suspend fun getMatchingCredentials(preparationState: AuthorizationResponsePreparationState) = catching {
        when (val it = preparationState.credentialPresentationRequest) {
            is CredentialPresentationRequest.DCQLRequest -> DCQLMatchingResult(
                presentationRequest = it,
                holderAgent.matchDCQLQueryAgainstCredentialStore(it.dcqlQuery).getOrThrow()
            )

            is CredentialPresentationRequest.PresentationExchangeRequest -> PresentationExchangeMatchingResult(
                presentationRequest = it,
                holderAgent.matchInputDescriptorsAgainstCredentialStore(
                    inputDescriptors = it.presentationDefinition.inputDescriptors,
                    fallbackFormatHolder = it.fallbackFormatHolder,
                ).getOrThrow()
            )

            null -> TODO()
        }
    }

    suspend fun finalizeAuthorizationResponse(
        request: RequestParametersFrom<AuthenticationRequestParameters>,
        clientMetadata: RelyingPartyMetadata?,
        credentialPresentation: CredentialPresentation,
        isCrossDeviceFlow: Boolean,
    ) {
        presentationService.finalizeAuthorizationResponse(
            request = request,
            clientMetadata = clientMetadata,
            credentialPresentation = credentialPresentation,
            isCrossDeviceFlow = isCrossDeviceFlow,
        ).getOrThrow()
    }

    suspend fun finalizeDCAPIPreviewPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        dcApiRequest: DCAPIRequest
    ) {
        Napier.d("Finalizing DCAPI response")
        val previewRequest = PreviewRequest.deserialize(dcApiRequest.request).getOrThrow()

        val presentationResult = holderAgent.createPresentation(
            request =  PresentationRequestParameters(
                nonce = previewRequest.nonce,
                audience = dcApiRequest.callingOrigin ?: dcApiRequest.callingPackageName!!,
                calcIsoDeviceSignature = {
                    coseService.createSignedCose(
                        payload = it.encodeToByteArray(),
                        serializer = ByteArraySerializer(),
                        addKeyId = false
                    ).getOrElse { e ->
                        Napier.w("Could not create DeviceAuth for presentation", e)
                        throw PresentationException(e)
                    } to null
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation = presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse
            is CreatePresentationResult.SdJwt -> TODO("Credential type not yet supported for API use case")
            is CreatePresentationResult.Signed -> TODO("Credential type not yet supported for API use case")
        }

        platformAdapter.prepareDCAPICredentialResponse(deviceResponse.serialize(), dcApiRequest)
    }

    suspend fun finalizeLocalPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        finishFunction: (ByteArray) -> Unit,
        spName: String?
    ) {
        Napier.d("Finalizing local response")

        val presentationResult = holderAgent.createPresentation(
            request =  PresentationRequestParameters(
                nonce = "",
                audience = spName ?: "",
                calcIsoDeviceSignature = {
                    //TODO sign as required by specification
                    coseService.createSignedCose(
                        payload = it.encodeToByteArray(),
                        serializer = ByteArraySerializer(),
                        addKeyId = false
                    ).getOrElse { e ->
                        Napier.w("Could not create DeviceAuth for presentation", e)
                        throw PresentationException(e)
                    } to null
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation = presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse.serialize()
            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }

        finishFunction(deviceResponse)
    }

}