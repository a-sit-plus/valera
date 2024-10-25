package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import appLink
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_title
import compose_wallet_app.shared.generated.resources.snackbar_credential_loaded_successfully
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ui.navigation.Routes.LoadingRoute
import ui.navigation.Routes.Route

@Composable
fun IntentHandler(walletMain: WalletMain, navigate: (Route) -> Unit, navigateBack: () -> Unit){
    LaunchedEffect(appLink.value) {
        Napier.d("app link changed to ${appLink.value}")
        appLink.value?.let { link ->
            // resetting error service so that the intent can be displayed as intended
            walletMain.errorService.reset()

            Napier.d("new app link: ${link}")
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)

            if (pars.contains("error")) {
                runBlocking {
                    walletMain.errorService.emit(
                        Exception(pars["error_description"] ?: "Unknown Exception")
                    )
                }
                appLink.value = null
                return@LaunchedEffect
            }

            if (walletMain.provisioningService.redirectUri?.let { link.contains(it) } == true) {
                navigate(LoadingRoute)
                try {
                    walletMain.cryptoService.promptText =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                    walletMain.cryptoService.promptSubtitle =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                    walletMain.provisioningService.handleResponse(link)
                    walletMain.snackbarService.showSnackbar(
                        getString(Res.string.snackbar_credential_loaded_successfully)
                    )
                    navigateBack()
                    appLink.value = null
                } catch (e: Throwable) {
                    navigateBack()
                    walletMain.errorService.emit(e)
                    appLink.value = null
                }
                return@LaunchedEffect
            }

            // if this is not for provisioning, it must be an authorization request
            kotlin.run {
                val consentPageBuilder =
                    BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                        oidcSiopWallet = walletMain.presentationService.oidcSiopWallet
                    )

                consentPageBuilder(link).unwrap().onSuccess {
                    Napier.d("valid authentication request")
                    navigate(it)
                }.onFailure {
                    Napier.d("invalid authentication request")
                }
                appLink.value = null
                return@LaunchedEffect
            }
        }
    }
}