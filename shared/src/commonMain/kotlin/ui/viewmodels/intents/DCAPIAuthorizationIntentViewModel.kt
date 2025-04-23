package ui.viewmodels.intents

import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.data.oid4vp.Oid4vpDCAPIRequest
import at.asitplus.wallet.app.common.dcapi.data.preview.PreviewDCAPIRequest
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
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

    fun process() {
        val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData()
        val consentPageBuilder =
            BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()


        when (dcApiRequest) {
            is PreviewDCAPIRequest -> {
                walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
                    consentPageBuilder(dcApiRequest).unwrap().onSuccess {
                        onSuccess(it)
                    }.onFailure {
                        onFailure(it)
                    }
                }
            }

            is Oid4vpDCAPIRequest -> {
                walletMain.scope.launch(Dispatchers.Default + coroutineExceptionHandler) {
                    buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(dcApiRequest.request).unwrap()
                        .onSuccess {
                            AuthenticationViewRoute(
                                authenticationRequestParametersFromSerialized = it.authenticationRequestParametersFromSerialized,
                                authorizationPreparationStateSerialized = it.authorizationPreparationStateSerialized,
                                recipientLocation = it.recipientLocation,
                                isCrossDeviceFlow = true
                            )
                            onSuccess(it)
                        }.onFailure {
                            onFailure(it)
                        }
                }
            }
        }

    }
}