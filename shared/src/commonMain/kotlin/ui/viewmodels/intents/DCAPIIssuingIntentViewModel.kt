package ui.viewmodels.intents

import at.asitplus.catching
import at.asitplus.dcapi.issuance.decodeSingleDigitalCredentialOffer
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.routes.AddCredentialDcApiRoute
import ui.navigation.routes.Route

class DCAPIIssuingIntentViewModel(
    val walletMain: WalletMain,
    val uri: String,
    val onSuccess: (Route) -> Unit,
    val onFailure: (Throwable) -> Unit
) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        Napier.e("Exception occurred during DC API issuing invocation", error)
        onFailure(error)
    }

    fun process() = walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        catching {
            val issuingData = walletMain.platformAdapter.getCurrentDCAPIIssuingData().getOrThrow()
            val credentialOffer = issuingData.requestJson.decodeSingleDigitalCredentialOffer()
            onSuccess(AddCredentialDcApiRoute(credentialOffer))
        }.onFailure { onFailure(it) }
    }
}
