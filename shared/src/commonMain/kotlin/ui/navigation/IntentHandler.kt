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

enum class IntentType {
    ErrorIntent,
    ProvisioningIntent,
    AuthorizationIntent
}

@Composable
fun handleIntent(walletMain: WalletMain, navigate: (Route) -> Unit, navigateBack: () -> Unit){
    LaunchedEffect(appLink.value) {
        Napier.d("app link changed to ${appLink.value}")
        appLink.value?.let { link ->
            when (parseIntent(walletMain,link)) {
                IntentType.ProvisioningIntent -> {
                    navigate(LoadingRoute)
                    try {
                        walletMain.cryptoService.promptText =
                            getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                        walletMain.cryptoService.promptSubtitle =
                            getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                        walletMain.provisioningService.resumeWithAuthCode(link)
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

                IntentType.ErrorIntent -> {
                    val parameterIndex = link.indexOfFirst { it == '?' }
                    val pars = parseQueryString(link, startIndex = parameterIndex + 1)
                    runBlocking {
                        walletMain.errorService.emit(
                            Exception(pars["error_description"] ?: "Unknown Exception")
                        )
                    }
                    appLink.value = null
                    return@LaunchedEffect
                }

                IntentType.AuthorizationIntent -> {
                    kotlin.run {
                        val consentPageBuilder =
                            BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                                presentationService = walletMain.presentationService
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
    }
}

fun parseIntent(walletMain: WalletMain, url: String): IntentType {
    return if (walletMain.provisioningService.redirectUri?.let { url.contains(it) } == true) {
        IntentType.ProvisioningIntent
    } else if (url.contains("error")) {
        IntentType.ErrorIntent
    } else {
        IntentType.AuthorizationIntent
    }
}