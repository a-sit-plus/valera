package ui.viewmodels.intents

import DeferredErrorActionException
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import ui.navigation.routes.AddCredentialPreAuthnRoute
import ui.navigation.routes.Route

class DCAPICreationIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        Napier.w("Exception occurred during DC API creation invocation", error)
        onFailure(
            DeferredErrorActionException(
                onAcknowledge = {
                    walletMain.platformAdapter.prepareDCAPICreationResponse(error.message ?: "invalid request", false)
                },
                cause = error
            )
        )
    }

    fun process() = walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        val creationData = walletMain.platformAdapter.getCurrentDCAPICreationData().getOrThrow()
        val credentialOffer = parseCredentialOffer(creationData.requestJson)
        onSuccess(AddCredentialPreAuthnRoute(credentialOffer))
    }

    private suspend fun parseCredentialOffer(requestJson: String): CredentialOffer {
        val root = vckJsonSerializer.parseToJsonElement(requestJson)
        val requestsElement = root.digitalRequestsElement()
            ?: throw IllegalArgumentException("DC API: Missing digital requests")
        val matchingRequest = requestsElement.firstOrNull { it.isOpenId4VciRequest() }
            ?: throw IllegalArgumentException("DC API: No supported issuance request found")
        val dataElement = matchingRequest.dataElement()
            ?: throw IllegalArgumentException("DC API: Missing issuance request data")
        val dataString = when (dataElement) {
            is JsonPrimitive -> dataElement.content
            else -> dataElement.toString()
        }
        return walletMain.provisioningService.decodeCredentialOffer(dataString)
    }
}

private const val OPENID4VCI_PROTOCOL = "openid4vci-v1"

private fun JsonElement.digitalRequestsElement(): JsonArray? {
    val root = this as? JsonObject ?: return null
    val digitalRequests = (root["digital"] as? JsonObject)?.get("requests")
    val directRequests = root["requests"]
    return (digitalRequests as? JsonArray) ?: (directRequests as? JsonArray)
}

private fun JsonElement.isOpenId4VciRequest(): Boolean {
    val obj = this as? JsonObject ?: return false
    return obj["protocol"]?.jsonPrimitive?.content == OPENID4VCI_PROTOCOL
}

private fun JsonElement.dataElement(): JsonElement? {
    val obj = this as? JsonObject ?: return null
    return obj["data"]
}
