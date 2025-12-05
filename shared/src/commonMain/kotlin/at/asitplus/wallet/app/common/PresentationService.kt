package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.dcapi.DCAPIInfo
import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.sha256
import at.asitplus.iso.wrapInCborTag
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.io.Base64UrlStrict
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
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray
import kotlin.io.encoding.ExperimentalEncodingApi

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

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    suspend fun finalizeDCAPIIsoMdocPresentation(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        isoMdocRequest: IsoMdocRequest
    ): OpenId4VpWallet.AuthenticationSuccess {
        Napier.d("Finalizing DCAPI response")

        val hash = coseCompliantSerializer.encodeToByteArray(
            DCAPIInfo(isoMdocRequest.encryptionInfo, isoMdocRequest.callingOrigin)
        ).sha256()
        val handover = DCAPIHandover(type = "dcapi", hash = hash)
        val sessionTranscript = SessionTranscript.forDcApi(handover)

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(
                // TODO which nonce? isoMdocRequest.parsedEncryptionInfo.encryptionParameters.nonce?
                nonce = isoMdocRequest.encryptionInfo.encryptionParameters.nonce
                    .encodeToString(Base64UrlStrict),
                audience = isoMdocRequest.callingOrigin,
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
                            throw PresentationException(e)
                        }
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse
            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }
        val deviceResponseSerialized =
            coseCompliantSerializer.encodeToByteArray(deviceResponse) // TODO HPKE encryption multiplatform

        platformAdapter.prepareDCAPIIsoMdocCredentialResponse(
            deviceResponseSerialized,
            coseCompliantSerializer.encodeToByteArray(sessionTranscript),
            isoMdocRequest.encryptionInfo.encryptionParameters
        )
        return OpenId4VpWallet.AuthenticationSuccess()
    }

    fun finalizeOid4vpDCAPIPresentation(response: String) =
        platformAdapter.prepareDCAPIOid4vpCredentialResponse(response, true)

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
                            throw PresentationException(e)
                        }
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults[0]) {
            is CreatePresentationResult.DeviceResponse -> coseCompliantSerializer.encodeToByteArray(
                firstResult.deviceResponse
            )

            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }

        finishFunction(deviceResponse)
    }

}
