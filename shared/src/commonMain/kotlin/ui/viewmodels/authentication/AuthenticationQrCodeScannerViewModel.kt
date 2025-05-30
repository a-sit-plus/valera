package ui.viewmodels.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.navigation.routes.AuthenticationViewRoute

class AuthenticationQrCodeScannerViewModel(
    val walletMain: WalletMain,
    val errorService: ErrorService,
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
): ViewModel() {
    fun onScan(
        link: String,
        onSuccess: (AuthenticationViewRoute) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        Napier.d("onScan: $link")

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            errorService.emit(error)
            onFailure(error)
        }

        viewModelScope.launch(coroutineExceptionHandler) {
            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).getOrThrow()
                    .let {
                        AuthenticationViewRoute(
                            authenticationRequestParametersFromSerialized = it.authenticationRequestParametersFromSerialized,
                            authorizationPreparationStateSerialized = it.authorizationPreparationStateSerialized,
                            recipientLocation = it.recipientLocation,
                            isCrossDeviceFlow = true
                        )
                    }

            onSuccess(authenticationConsentPage)
        }
    }
}