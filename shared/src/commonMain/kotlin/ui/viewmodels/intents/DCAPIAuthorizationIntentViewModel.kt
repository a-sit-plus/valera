package ui.viewmodels.intents

import ErrorHandlingOverrideException
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import at.asitplus.wallet.app.common.dcapi.DCAPIVerificationData
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.openid.DcApiPreparationState
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.routes.Route

class DCAPIAuthorizationIntentViewModel(
    val walletMain: WalletMain,
    private val intentState: IntentState,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    private val buildConsentPageFromDcApiRequest =
        BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        Napier.w("Exception occurred during DC API invocation", error)
        val response = when (error) {
            is OAuth2Exception -> error
            else -> OAuth2Exception.InvalidRequest(error.message) // TODO Not sure what to return in this case
        }.serialize()
        onFailure(
            ErrorHandlingOverrideException(
                onAcknowledge = {
                    walletMain.platformAdapter.prepareDCAPICredentialError(response)
                },
                cause = error
            )
        )
    }

    fun process() = walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        val successRoute = when (
            val data = walletMain.platformAdapter.getCurrentDCAPIVerificationData().getOrThrow()
        ) {
            is DCAPIVerificationData.Presentation -> {
                val missingTypes = findMissingIssuableCredentialTypes(data)
                if (missingTypes.isEmpty()) {
                    buildConsentPageFromDcApiRequest(data.request).getOrThrow()
                } else {
                    intentState.pendingDCAPIVerificationIssuanceQueue.value = missingTypes
                    ui.navigation.routes.AddCredentialForDCAPIVerificationRoute(missingTypes.first())
                }
            }

            is DCAPIVerificationData.IssuanceRequired ->
                ui.navigation.routes.AddCredentialForDCAPIVerificationRoute(data.credentialType)
        }

        onSuccess(successRoute)
    }

    private suspend fun findMissingIssuableCredentialTypes(
        data: DCAPIVerificationData.Presentation,
    ): List<DCAPICredentialType> {
        val supportedTypes = walletMain.platformAdapter.dcApiVerificationIssuanceTypes
        if (supportedTypes.isEmpty()) return emptyList()

        val preparationState = walletMain.presentationService.prepareDcApiRequest(data.request).getOrThrow()
        if (preparationState !is DcApiPreparationState.Iso180137AnnexC) return emptyList()
        val matchingResult = walletMain.presentationService.getMatchingCredentials(preparationState).getOrThrow()
            as? IsoDeviceRetrievalMatchingResult<*> ?: return emptyList()
        return missingIssuableCredentialTypes(
            requestedDocTypes = preparationState.presentationRequest.deviceRequest.docRequests.map {
                it.itemsRequest.value.docType
            },
            hasMatches = matchingResult.matchingResult.documentMatches.map { it.isNotEmpty() },
            supportedTypes = supportedTypes,
        )
    }
}

internal fun missingIssuableCredentialTypes(
    requestedDocTypes: List<String>,
    hasMatches: List<Boolean>,
    supportedTypes: Set<DCAPICredentialType>,
): List<DCAPICredentialType> {
    require(requestedDocTypes.size == hasMatches.size) { "Request and match counts differ" }
    val missing = mutableListOf<DCAPICredentialType>()
    requestedDocTypes.forEachIndexed { index, docType ->
        if (!hasMatches[index]) {
            val type = DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, docType)
            if (type in supportedTypes && type !in missing) missing += type
        }
    }
    return missing
}
