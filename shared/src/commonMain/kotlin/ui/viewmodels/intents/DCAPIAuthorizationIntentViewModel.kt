package ui.viewmodels.intents

import ErrorHandlingOverrideException
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.routes.Route

class DCAPIAuthorizationIntentViewModel(
    val walletMain: WalletMain,
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
        val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIVerificationData().getOrThrow()

        val successRoute = buildConsentPageFromDcApiRequest(dcApiRequest).getOrThrow()

        onSuccess(successRoute)
    }
}
