package ui.navigation

import appLink
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_title
import at.asitplus.valera.resources.snackbar_credential_loaded_successfully
import at.asitplus.wallet.app.common.SigningState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ui.navigation.Routes.HomeScreenRoute
import ui.navigation.Routes.LoadingRoute
import ui.navigation.Routes.Route

enum class IntentType {
    ErrorIntent,
    ProvisioningIntent,
    AuthorizationIntent,
    SigningServiceIntent,
    SigningCredentialIntent,
    SiginingPreloadIntent,
    SigningIntent,
    DCAPIAuthorizationIntent
}

suspend fun handleIntent(
    walletMain: WalletMain,
    navigate: (Route) -> Unit,
    navigateBack: () -> Unit,
    link: String
) {
    Napier.d("app link changed to ${link}")
    when (parseIntent(walletMain, link)) {
        IntentType.ProvisioningIntent -> {
            navigate(LoadingRoute)
            try {
                walletMain.cryptoService.promptText =
                    getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                walletMain.cryptoService.promptSubtitle =
                    getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                walletMain.provisioningService.resumeWithAuthCode(link)
                walletMain.snackbarService.showSnackbar(getString(Res.string.snackbar_credential_loaded_successfully))
                navigateBack()
                appLink.value = null
            } catch (e: Throwable) {
                navigateBack()
                walletMain.errorService.emit(e)
                appLink.value = null
            }
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
        }

        IntentType.AuthorizationIntent -> {
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
        }

        IntentType.DCAPIAuthorizationIntent -> {
            val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData()
            val consentPageBuilder =
                BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()

            consentPageBuilder(dcApiRequest).unwrap().onSuccess {
                Napier.d("valid authentication request")
                navigate(it)
            }.onFailure {
                walletMain.errorService.emit(Exception("Invalid Authentication Request"))
            }

            appLink.value = null
        }

        IntentType.SigningServiceIntent -> {
            runCatching {
                walletMain.signingService.resumeWithServiceAuthCode(url = link)
            }.onSuccess {
                navigate(HomeScreenRoute)
            }.onFailure { e ->
                walletMain.errorService.emit(e)
            }
        }
        IntentType.SiginingPreloadIntent -> {
            runCatching {
                walletMain.signingService.resumePreloadCertificate(url = link)
            }.onFailure { e ->
                walletMain.errorService.emit(e)
            }
        }
        IntentType.SigningCredentialIntent -> {
            runCatching {
                walletMain.signingService.resumeWithCredentialAuthCode(url = link)
            }.onSuccess {
                navigate(HomeScreenRoute)
            }.onFailure { e ->
                walletMain.errorService.emit(e)
            }
        }
        IntentType.SigningIntent -> {
            runCatching {
                walletMain.signingService.start(link)
            }.onFailure { e ->
                walletMain.errorService.emit(e)
            }
        }
    }
}


fun parseIntent(walletMain: WalletMain, url: String): IntentType {
    return if((walletMain.signingService.redirectUri?.let { url.contains(it) } == true)) {
        when (walletMain.signingService.state) {
            SigningState.ServiceRequest -> IntentType.SigningServiceIntent
            SigningState.CredentialRequest -> IntentType.SigningCredentialIntent
            SigningState.PreloadCredential -> IntentType.SiginingPreloadIntent
            null -> TODO()
        }.also { walletMain.signingService.state = null }
    } else if (walletMain.provisioningService.redirectUri?.let { url.contains(it) } == true) {
        IntentType.ProvisioningIntent
    } else if (url.contains("error")) {
        IntentType.ErrorIntent
    } else if (url == "androidx.identitycredentials.action.GET_CREDENTIALS") {
        IntentType.DCAPIAuthorizationIntent
    } else if (url.contains("createSignRequest")) {
        IntentType.SigningIntent
    } else {
        IntentType.AuthorizationIntent
    }
}