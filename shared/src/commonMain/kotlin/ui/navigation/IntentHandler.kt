package ui.navigation

import Route
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import appLink
import at.asitplus.wallet.app.common.WalletMain
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking

@Composable
fun IntentHandler(walletMain: WalletMain, navigate: (Route) -> Unit){
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
                walletMain.provisioningService.redirectUri = null
                navigate(ProvisioningLoadingRoute(link = link))
                appLink.value = null
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