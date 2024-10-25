package view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.wallet.app.common.WalletMain
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.Routes.AuthenticationConsentRoute

class AuthenticationQrCodeScannerViewModel(
    val navigateUp: (() -> Unit)?,
    val onSuccess: (AuthenticationConsentRoute) -> Unit,
    val walletMain: WalletMain
) {
    var isLoading by mutableStateOf(false)
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
        oidcSiopWallet = walletMain.presentationService.oidcSiopWallet,
    )

    fun onScan(link: String) {
        Napier.d("onScan: $link")

        isLoading = true
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            isLoading = false
            walletMain.errorService.emit(error)
        }

        CoroutineScope(Dispatchers.Main).launch(coroutineExceptionHandler) {
            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).getOrThrow().let {
                    AuthenticationConsentRoute(
                        authenticationRequestParametersFromSerialized = it.authenticationRequestParametersFromSerialized,
                        authorizationPreparationStateSerialized = it.authorizationPreparationStateSerialized,
                        recipientLocation = it.recipientLocation,
                    )
                }

            isLoading = false
            onSuccess(authenticationConsentPage)
        }
    }
}