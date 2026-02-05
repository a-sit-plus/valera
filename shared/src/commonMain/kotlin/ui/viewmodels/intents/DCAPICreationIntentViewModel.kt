package ui.viewmodels.intents

import DeferredErrorActionException
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.dcapi.issuance.DigitalCredentialCreationOptions
import at.asitplus.openid.CredentialOffer
import at.asitplus.wallet.lib.data.vckJsonSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.routes.AddCredentialPreAuthnDcApiRoute
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
        catching {
            val creationData = walletMain.platformAdapter.getCurrentDCAPICreationData().getOrThrow()
            val credentialOffer = parseCredentialOffer(creationData.requestJson).getOrThrow()
            onSuccess(AddCredentialPreAuthnDcApiRoute(credentialOffer))
        }.onFailure { onFailure(it) }
    }

    private fun parseCredentialOffer(requestJson: String): KmmResult<CredentialOffer> = catching {
        val creationOptions =
            vckJsonSerializer.decodeFromString<DigitalCredentialCreationOptions>(requestJson)
        // TODO specification does not yet define what to do with multiple requests
        // TODO parse and check origin once its sent by the web platform ("… will send the credential offer along with the Origin of the Issuer to the End-User's chosen Wallet.")
        require(creationOptions.requests.count() == 1) { "Only one request supported for now" }
        creationOptions.requests.firstOrNull()?.data
            ?: throw IllegalArgumentException("DC API: No supported issuance request found")
    }
}
