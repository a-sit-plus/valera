package view

import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.navigation.AuthenticationConsentPage

class AuthenticationQrCodeScannerViewModel(
    private val buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase: BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase,
) {
    constructor(
        oidcSiopWallet: OidcSiopWallet,
        consentPageBuilder: AuthenticationConsentPage.Companion.Builder,
    ) : this(
        buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase = BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
            oidcSiopWallet = oidcSiopWallet,
            consentPageBuilder = consentPageBuilder,
            fromQrCodeScanner = true,
        ),
    )

    fun onScan(
        link: String,
        startLoadingCallback: () -> Unit,
        stopLoadingCallback: () -> Unit,
        onSuccess: (AuthenticationConsentPage) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        Napier.d("onScan: $link")

        startLoadingCallback()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            stopLoadingCallback()
            onFailure(error)
        }

        CoroutineScope(Dispatchers.Main).launch(coroutineExceptionHandler) {
            val authenticationConsentPage =
                buildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(link).getOrThrow()

            stopLoadingCallback()
            onSuccess(authenticationConsentPage)
        }
    }
}