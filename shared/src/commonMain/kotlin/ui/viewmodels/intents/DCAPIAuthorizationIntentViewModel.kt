package ui.viewmodels.intents

import at.asitplus.dcapi.request.IsoMdocRequest
import at.asitplus.dcapi.request.Oid4vpDCAPIRequest
import at.asitplus.dcapi.request.PreviewDCAPIRequest
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import at.asitplus.wallet.lib.oidvci.OAuth2Exception
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
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
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase =
        BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            presentationService = walletMain.presentationService,
        )

    private val buildAuthenticationConsentPageFromPreviewRequest =
        BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        Napier.w("Exception occurred during DC API invocation", error)
        val response = when (error) {
            is OAuth2Exception -> error
            else -> OAuth2Exception.InvalidRequest(error.message) // TODO Not sure what to return in this case
        }.serialize()
        walletMain.platformAdapter.prepareDCAPIOid4vpCredentialResponse(response, false)
        onFailure(error)
    }

    fun process() = walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData().getOrThrow()

        val successRoute = when (dcApiRequest) {
            is PreviewDCAPIRequest -> 
                buildAuthenticationConsentPageFromPreviewRequest(dcApiRequest)
            
            is Oid4vpDCAPIRequest ->
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                    dcApiRequest.request,
                    dcApiRequest
                )

            is IsoMdocRequest ->
                buildAuthenticationConsentPageFromPreviewRequest(dcApiRequest)

        }.getOrThrow()

        onSuccess(successRoute)
    }
}
