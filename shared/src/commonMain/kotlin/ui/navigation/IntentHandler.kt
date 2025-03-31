package ui.navigation

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_title
import at.asitplus.valera.resources.snackbar_credential_loaded_successfully
import at.asitplus.wallet.app.common.SigningState
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.LoadingRoute
import ui.navigation.routes.Route

enum class IntentType {
    ErrorIntent,
    ProvisioningIntent,
    AuthorizationIntent,
    DCAPIAuthorizationIntent,
    PresentationIntent,
    SigningServiceIntent,
    SigningCredentialIntent,
    SiginingPreloadIntent,
    SigningIntent,
}

const val PRESENTATION_REQUESTED_INTENT = "PRESENTATION_REQUESTED"
const val GET_CREDENTIALS_INTENT = "androidx.identitycredentials.action.GET_CREDENTIALS"

suspend fun handleIntent(
    walletMain: WalletMain,
    navigate: (Route) -> Unit,
    navigateBack: () -> Unit,
    link: String
) {
    Napier.d("app link changed to $link")
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
            } catch (e: Throwable) {
                navigateBack()
                walletMain.errorService.emit(e)
            }
            Globals.appLink.value = null
        }

        IntentType.ErrorIntent -> {
            val parameterIndex = link.indexOfFirst { it == '?' }
            val pars = parseQueryString(link, startIndex = parameterIndex + 1)
            runBlocking {
                walletMain.errorService.emit(
                    Exception(pars["error_description"] ?: "Unknown Exception")
                )
                Globals.appLink.value = null
            }
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
            Globals.appLink.value = null
        }

        IntentType.DCAPIAuthorizationIntent -> {
            val dcApiRequest = walletMain.platformAdapter.getCurrentDCAPIData()
            val consentPageBuilder =
                BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()

            consentPageBuilder(dcApiRequest).unwrap().onSuccess {
                Napier.d("valid DCAPI authentication request")
                navigate(it)
            }.onFailure {
                walletMain.errorService.emit(Exception("Invalid Authentication Request"))
            }
            Globals.appLink.value = null
        }

        IntentType.PresentationIntent -> {
            val consentPageBuilder =
                BuildAuthenticationConsentPageFromAuthenticationRequestLocalPresentment()

            consentPageBuilder(PresentationRequest(PRESENTATION_REQUESTED_INTENT)).unwrap().onSuccess {
                Napier.d("valid presentation request")
                navigate(it)
            }.onFailure {
                walletMain.errorService.emit(Exception("Invalid Authentication Request"))
            }
            Globals.appLink.value = null
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
    return if (url.contains("error")) {
        IntentType.ErrorIntent
    } else if((walletMain.signingService.redirectUri?.let { url.contains(it) } == true)) {
        when (walletMain.signingService.state) {
            SigningState.ServiceRequest -> IntentType.SigningServiceIntent
            SigningState.CredentialRequest -> IntentType.SigningCredentialIntent
            SigningState.PreloadCredential -> IntentType.SiginingPreloadIntent
            null -> throw Throwable("Missing state in SigningService")
        }.also { walletMain.signingService.state = null }
    } else if (walletMain.provisioningService.redirectUri?.let { url.contains(it) } == true) {
        IntentType.ProvisioningIntent
    } else if (url == GET_CREDENTIALS_INTENT) {
        IntentType.DCAPIAuthorizationIntent
    } else if (url.contains("createSignRequest")) {
        IntentType.SigningIntent
    } else if (url == PRESENTATION_REQUESTED_INTENT) {
        IntentType.PresentationIntent
    } else {
        IntentType.AuthorizationIntent
    }
}