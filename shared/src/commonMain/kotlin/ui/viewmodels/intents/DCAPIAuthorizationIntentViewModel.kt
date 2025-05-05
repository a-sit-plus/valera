package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.data.request.Oid4vpDCAPIRequest
import at.asitplus.wallet.app.common.dcapi.data.request.PreviewDCAPIRequest
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.routes.AuthenticationViewRoute
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
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
        onFailure(error)
    }

    fun process() = walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData().getOrThrow()
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()


        when (dcApiRequest) {
            is PreviewDCAPIRequest -> {
                consentPageBuilder(dcApiRequest).unwrap().onSuccess {
                    onSuccess(it)
                }.onFailure {
                    onFailure(it)
                }
            }

            is Oid4vpDCAPIRequest -> {
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(dcApiRequest.request).unwrap()
                    .onSuccess {
                        onSuccess(it)
                    }.onFailure {
                        onFailure(it)
                    }
            }

            null -> throw IllegalStateException("DC API request is null")
        }

    }
}