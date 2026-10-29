package at.asitplus.wallet.app.common

import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.SessionTranscript
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.app.common.presentation.LongfellowZkRegistration
import at.asitplus.wallet.app.common.presentation.PresentationRequestStore
import at.asitplus.wallet.app.common.validation.ValidationPolicies
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpKtorHolder
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import at.asitplus.wallet.lib.validation.RelyingPartyValidator
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray

class PresentationService(
    val platformAdapter: PlatformAdapter,
    val keyMaterial: WalletKeyMaterial,
    val holderAgent: HolderAgent,
    httpService: HttpService,
    settingsRepository: SettingsRepository,
    private val relyingPartyValidator: RelyingPartyValidator,
) {
    private val client = httpService.buildHttpClient()

    /** The requests validated in this session, until their consent screen is left, see [PresentationRequestStore]. */
    val validatedRequests = PresentationRequestStore()
    private val presentationService = OpenId4VpKtorHolder(
        httpClient = client,
        keyMaterial = keyMaterial,
        holder = holderAgent,
        allowedDcApiOriginSchemes = { settingsRepository.openId4VpAllowedOriginSchemes.first() },
        relyingPartyValidator = relyingPartyValidator,
    )

    fun close() {
        client.close()
        // HttpService supplies a dedicated engine; cancelling it also stops VC-K's client copies.
        client.engine.close()
        client.engine.cancel()
    }

    /**
     * Loads and validates the request [input] under [ValidationPolicies.strict], returning its report next to the
     * state to answer it with, also if the request is rejected, so that the user can decide whether to continue.
     * Request parsing and certificate trust validation run off the UI dispatcher, like the other validation entry points.
     */
    suspend fun startAuthorizationResponsePreparation(input: String) =
        withContext(Dispatchers.Default) {
            presentationService.startAuthorizationResponsePreparation(input, ValidationPolicies.strict)
        }

    /** Validates [input] under [ValidationPolicies.strict], see the overload taking a string. */
    suspend fun startAuthorizationResponsePreparation(input: RequestParametersFrom<AuthenticationRequestParameters>) =
        withContext(Dispatchers.Default) {
            presentationService.startAuthorizationResponsePreparation(input, ValidationPolicies.strict)
        }

    suspend fun getMatchingCredentials(
        preparationState: AuthorizationResponsePreparationState
    ) = presentationService.getMatchingCredentials(preparationState)

    /**
     * Validates the relying party of the proximity request [deviceRequest] under [ValidationPolicies.strict]: its
     * reader authentication over [sessionTranscript] with the WRPAC, and its WRPRCs.
     */
    suspend fun validateRelyingParty(deviceRequest: DeviceRequest, sessionTranscript: SessionTranscript) =
        withContext(Dispatchers.Default) {
            relyingPartyValidator.validate(deviceRequest, sessionTranscript, ValidationPolicies.strict)
        }

    /** Validates [request] under [ValidationPolicies.strict], returning its report next to the state. */
    suspend fun prepareDcApiRequest(request: RequestParametersFrom.DcApiRequest) =
        withContext(Dispatchers.Default) {
            presentationService.prepareDcApiRequest(request, ValidationPolicies.strict)
        }

    suspend fun getMatchingCredentials(preparationState: DcApiPreparationState) =
        presentationService.getMatchingCredentials(preparationState)

    suspend fun finalizeAuthorizationResponse(
        credentialPresentation: CredentialPresentation,
        preparationState: AuthorizationResponsePreparationState,
    ): OpenId4VpKtorHolder.AuthenticationResult {
        LongfellowZkRegistration.ensureRegisteredFor(credentialPresentation)
        return presentationService.finalizeAuthorizationResponse(
            credentialPresentation = credentialPresentation,
            preparationState = preparationState
        ).getOrThrow()
    }

    suspend fun finalizeDcApiPresentation(
        credentialPresentation: CredentialPresentation,
        preparationState: DcApiPreparationState,
    ) {
        Napier.d("Finalizing DCAPI response")
        LongfellowZkRegistration.ensureRegisteredFor(credentialPresentation)
        val response = presentationService.finalizeDcApiResponse(
            state = preparationState,
            credentialPresentation = credentialPresentation,
        ).getOrThrow()
        platformAdapter.prepareDCAPICredentialResponse(response)
    }

    suspend fun finalizeLocalPresentation(
        credentialPresentation: CredentialPresentation.IsoDeviceRetrievalPresentation,
        finishFunction: (ByteArray) -> Unit,
        spName: String?,
        sessionTranscript: SessionTranscript
    ) {
        Napier.d("Finalizing local response")
        LongfellowZkRegistration.ensureRegisteredFor(credentialPresentation)

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(
                nonce = "",
                audience = spName ?: "",
                calcIsoSessionTranscript = { sessionTranscript },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.DeviceRetrievalParameters

        val deviceResponse = coseCompliantSerializer.encodeToByteArray(presentation.deviceResponse)

        Napier.d("Local presentation created device response with ${deviceResponse.size} bytes")
        finishFunction(deviceResponse)
        Napier.d("Local presentation handed device response back to presenter")
    }

}
