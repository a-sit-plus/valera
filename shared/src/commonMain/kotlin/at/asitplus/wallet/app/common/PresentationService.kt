package at.asitplus.wallet.app.common

import at.asitplus.catching
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RelyingPartyMetadata
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.dcapi.preview.DCAPIRequest
import at.asitplus.wallet.app.common.dcapi.preview.PreviewRequest
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationException
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.cbor.CoseHeaderNone
import at.asitplus.wallet.lib.cbor.SignCose
import at.asitplus.wallet.lib.cbor.SignCoseDetached
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.SessionTranscript
import at.asitplus.wallet.lib.iso.wrapInCborTag
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray
import ui.viewmodels.authentication.DCQLMatchingResult
import ui.viewmodels.authentication.PresentationExchangeMatchingResult

class PresentationService(
    val platformAdapter: PlatformAdapter,
    val keyMaterial: WalletKeyMaterial,
    val holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val presentationService = OpenId4VpWallet(
        engine = HttpClient().engine,
        httpClientConfig = httpService.loggingConfig,
        keyMaterial = keyMaterial,
        holderAgent = holderAgent
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
    ) = presentationService.finalizeAuthorizationResponse(
        request = request,
        clientMetadata = clientMetadata,
        credentialPresentation = credentialPresentation,
    ).getOrThrow()

    suspend fun finalizeDCAPIPreviewPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        dcApiRequest: DCAPIRequest
    ): OpenId4VpWallet.AuthenticationSuccess {
        Napier.d("Finalizing DCAPI response")
        val previewRequest = PreviewRequest.deserialize(dcApiRequest.request).getOrThrow()

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(
                nonce = previewRequest.nonce,
                audience = dcApiRequest.callingOrigin ?: dcApiRequest.callingPackageName!!,
                calcIsoDeviceSignature = { docType, deviceNameSpaceBytes ->
                    // TODO sign data
                    SignCose<ByteArray>(keyMaterial, CoseHeaderNone(), CoseHeaderNone())
                        .invoke(null, null, docType.encodeToByteArray(), ByteArraySerializer())
                        .getOrElse { e ->
                            Napier.w("Could not create DeviceAuth for presentation", e)
                            throw PresentationException(e)
                        } to null
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse
            is CreatePresentationResult.SdJwt -> TODO("Credential type not yet supported for API use case")
            is CreatePresentationResult.Signed -> TODO("Credential type not yet supported for API use case")
        }

        platformAdapter.prepareDCAPICredentialResponse(coseCompliantSerializer.encodeToByteArray(deviceResponse), dcApiRequest)

        return OpenId4VpWallet.AuthenticationSuccess()
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun finalizeLocalPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        finishFunction: (ByteArray) -> Unit,
        spName: String?,
        sessionTranscript: SessionTranscript
    ) {
        Napier.d("Finalizing local response")

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(
                nonce = "",
                audience = spName ?: "",
                calcIsoDeviceSignature = { docType, deviceNameSpaceBytes ->
                    val deviceAuthentication = DeviceAuthentication(
                        type = "DeviceAuthentication",
                        sessionTranscript = sessionTranscript, docType = docType,
                        namespaces = deviceNameSpaceBytes
                    )

                    val deviceAuthenticationBytes = coseCompliantSerializer
                        .encodeToByteArray(ByteStringWrapper(deviceAuthentication))
                        .wrapInCborTag(24)
                    Napier.d("Device authentication signature input is ${deviceAuthenticationBytes.toHexString()}")

                    SignCoseDetached<ByteArray>(keyMaterial, CoseHeaderNone(), CoseHeaderNone())
                        .invoke(null, null, deviceAuthenticationBytes, ByteArraySerializer())
                        .getOrElse { e ->
                            Napier.w("Could not create DeviceAuth for presentation", e)
                            throw PresentationException(e)
                        } to null
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> coseCompliantSerializer.encodeToByteArray(firstResult.deviceResponse)
            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }

        finishFunction(deviceResponse)
    }

}
