package ui.navigation

import appLink
import at.asitplus.catchingUnwrapped
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.LoadingRoute
import ui.navigation.routes.Route

class IntentService(
    val walletMain: WalletMain,
    val navigate: (Route) -> Unit,
    val navigateBack: () -> Unit) {

    val readyForIntents = MutableStateFlow<Boolean?>(null)

    suspend fun handleIntent(uri: String) {
        Napier.d("New intent: $uri")
        when (parseIntent(uri)) {
            IntentType.ProvisioningIntent -> {
                navigate(LoadingRoute)

                catchingUnwrapped{
                    walletMain.cryptoService.promptText =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                    walletMain.cryptoService.promptSubtitle =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                    walletMain.provisioningService.resumeWithAuthCode(uri)
                    walletMain.snackbarService.showSnackbar(getString(Res.string.snackbar_credential_loaded_successfully))
                    navigateBack()
                    appLink.value = null
                }.onFailure {
                    navigateBack()
                    walletMain.errorService.emit(it)
                    appLink.value = null
                }
            }

            IntentType.ErrorIntent -> {
                val parameterIndex = uri.indexOfFirst { it == '?' }
                val pars = parseQueryString(uri, startIndex = parameterIndex + 1)
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

                consentPageBuilder(uri).unwrap().onSuccess {
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
                catchingUnwrapped {
                    walletMain.signingService.resumeWithServiceAuthCode(url = uri)
                }.onSuccess {
                    navigate(HomeScreenRoute)
                }.onFailure { e ->
                    walletMain.errorService.emit(e)
                }
            }
            IntentType.SigningPreloadIntent -> {
                catchingUnwrapped {
                    walletMain.signingService.resumePreloadCertificate(url = uri)
                }.onFailure { e ->
                    walletMain.errorService.emit(e)
                }
            }
            IntentType.SigningCredentialIntent -> {
                catchingUnwrapped {
                    walletMain.signingService.resumeWithCredentialAuthCode(url = uri)
                }.onSuccess {
                    navigate(HomeScreenRoute)
                }.onFailure { e ->
                    walletMain.errorService.emit(e)
                }
            }
            IntentType.SigningIntent -> {
                catchingUnwrapped {
                    walletMain.signingService.start(uri)
                }.onFailure { e ->
                    walletMain.errorService.emit(e)
                }
            }
        }
    }

    fun parseIntent(uri: String): IntentType {
        return if (uri.contains("error")) {
            IntentType.ErrorIntent
        } else if((walletMain.signingService.redirectUri?.let { uri.contains(it) } == true)) {
            when (walletMain.signingService.state) {
                SigningState.ServiceRequest -> IntentType.SigningServiceIntent
                SigningState.CredentialRequest -> IntentType.SigningCredentialIntent
                SigningState.PreloadCredential -> IntentType.SigningPreloadIntent
                null -> throw Throwable("Missing state in SigningService")
            }.also { walletMain.signingService.state = null }
        } else if (walletMain.provisioningService.redirectUri?.let { uri.contains(it) } == true) {
            IntentType.ProvisioningIntent
        } else if (uri == "androidx.identitycredentials.action.GET_CREDENTIALS") {
            IntentType.DCAPIAuthorizationIntent
        } else if (uri.contains("createSignRequest")) {
            IntentType.SigningIntent
        } else {
            IntentType.AuthorizationIntent
        }
    }

    enum class IntentType {
        ErrorIntent,
        ProvisioningIntent,
        AuthorizationIntent,
        SigningServiceIntent,
        SigningCredentialIntent,
        SigningPreloadIntent,
        SigningIntent,
        DCAPIAuthorizationIntent
    }
}