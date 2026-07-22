package at.asitplus.wallet.app.common

import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.wrapInCborTag
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.supreme.UserInitiatedCancellationReason
import at.asitplus.wallet.app.common.data.SettingsRepository
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
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray

class PresentationService(
    val platformAdapter: PlatformAdapter,
    val keyMaterial: WalletKeyMaterial,
    val holderAgent: HolderAgent,
    httpService: HttpService,
    settingsRepository: SettingsRepository,
) {
    private val presentationService = OpenId4VpWallet(
        engine = HttpClient().engine,
        httpClientConfig = httpService.loggingConfig,
        keyMaterial = keyMaterial,
        holderAgent = holderAgent,
        allowedDcApiOriginSchemes = { settingsRepository.openId4VpAllowedOriginSchemes.first() },
    )

    suspend fun startAuthorizationResponsePreparation(input: String) =
        presentationService.startAuthorizationResponsePreparation(input)

    suspend fun startAuthorizationResponsePreparation(input: RequestParametersFrom<AuthenticationRequestParameters>) =
        presentationService.startAuthorizationResponsePreparation(input)

    suspend fun getMatchingCredentials(
        preparationState: AuthorizationResponsePreparationState
    ) = presentationService.getMatchingCredentials(preparationState)

    suspend fun prepareDcApiRequest(request: RequestParametersFrom.DcApiRequest) =
        presentationService.prepareDcApiRequest(request)

    suspend fun getMatchingCredentials(preparationState: DcApiPreparationState) =
        presentationService.getMatchingCredentials(preparationState)

    suspend fun finalizeAuthorizationResponse(
        credentialPresentation: CredentialPresentation,
        preparationState: AuthorizationResponsePreparationState,
    ) = presentationService.finalizeAuthorizationResponse(
        credentialPresentation = credentialPresentation,
        preparationState = preparationState
    ).getOrThrow()

    suspend fun finalizeDcApiPresentation(
        credentialPresentation: CredentialPresentation,
        preparationState: DcApiPreparationState,
    ) {
        Napier.d("Finalizing DCAPI response")
        val response = presentationService.finalizeDcApiResponse(
            state = preparationState,
            credentialPresentation = credentialPresentation,
        ).getOrThrow()
        platformAdapter.prepareDCAPICredentialResponse(response)
    }

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

        val deviceResponse = when (val result = presentation.presentationResults.singleOrNull()
            ?: throw PresentationException(
                IllegalStateException("Local presentation must return exactly one device response")
            )) {
            is CreatePresentationResult.DeviceResponse -> coseCompliantSerializer.encodeToByteArray(
                result.deviceResponse
            )

            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }

        Napier.d("Local presentation created device response with ${deviceResponse.size} bytes")
        finishFunction(deviceResponse)
        Napier.d("Local presentation handed device response back to presenter")
    }

}
