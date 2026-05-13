package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.dcapi.DCAPIHandover.Companion.TYPE_DCAPI
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.dcapi.DCAPIInfo
import at.asitplus.dcapi.EncryptedResponse
import at.asitplus.dcapi.EncryptedResponseData
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.wrapInCborTag
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationException
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.cbor.CoseHeaderNone
import at.asitplus.wallet.lib.cbor.SignCoseDetached
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray

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

    suspend fun startAuthorizationResponsePreparation(input: String) =
        presentationService.startAuthorizationResponsePreparation(input)

    suspend fun startAuthorizationResponsePreparation(input: DCAPIWalletRequest.OpenId4Vp) =
        presentationService.startAuthorizationResponsePreparation(input)

    suspend fun getMatchingCredentials(
        preparationState: AuthorizationResponsePreparationState
    ) = presentationService.getMatchingCredentials(preparationState)

    suspend fun finalizeAuthorizationResponse(
        credentialPresentation: CredentialPresentation,
        preparationState: AuthorizationResponsePreparationState,
    ) = presentationService.finalizeAuthorizationResponse(
        credentialPresentation = credentialPresentation,
        preparationState = preparationState
    ).getOrThrow()

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun finalizeIsoMdocDCAPIPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        isoMdocWalletRequest: DCAPIWalletRequest.IsoMdoc
    ): OpenId4VpWallet.AuthenticationSuccess {
        Napier.d("Finalizing DCAPI response")
        val encryptedResponse = IsoMdocDcapiResponseBuilder.buildEncryptedResponse(
            credentialPresentation = credentialPresentation,
            isoMdocWalletRequest = isoMdocWalletRequest,
            keyMaterial = keyMaterial,
            holderAgent = holderAgent,
        )
        platformAdapter.prepareIsoMdocDCAPICredentialResponse(encryptedResponse, true)
        return OpenId4VpWallet.AuthenticationSuccess()
    }

    fun finalizeOpenId4VpDCAPIPresentation(response: String) =
        platformAdapter.prepareDCAPICredentialResponse(response, true)

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
                returnOneDeviceResponse = true,
                calcIsoDeviceSignaturePlain = { input ->
                    val deviceAuthentication = DeviceAuthentication(
                        type = "DeviceAuthentication",
                        sessionTranscript = sessionTranscript,
                        docType = input.docType,
                        namespaces = input.deviceNameSpaceBytes
                    )

                    val deviceAuthenticationBytes = coseCompliantSerializer
                        .encodeToByteArray(ByteStringWrapper(deviceAuthentication))
                        .wrapInCborTag(24)
                    Napier.d("Device authentication signature input is ${deviceAuthenticationBytes.toHexString()}")
                    SignCoseDetached<ByteArray>(keyMaterial, CoseHeaderNone(), CoseHeaderNone())
                        .invoke(null, null, deviceAuthenticationBytes, ByteArraySerializer())
                        .getOrElse { e ->
                            Napier.w("Could not create DeviceAuth for presentation", e)
                            // Unwrap user cancellation (e.g. biometric dismissed) so callers can
                            // treat it separately from real errors.
                            throw generateSequence(e as Throwable?) { it.cause }
                                .filterIsInstance<UserInitiatedCancellationReason>()
                                .firstOrNull() ?: PresentationException(e)
                        }
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults.firstOrNull()
            ?: throw PresentationException(IllegalStateException("Presentation did not return any device response"))) {
            is CreatePresentationResult.DeviceResponse -> coseCompliantSerializer.encodeToByteArray(
                firstResult.deviceResponse
            )

            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }

        finishFunction(deviceResponse)
    }

}
