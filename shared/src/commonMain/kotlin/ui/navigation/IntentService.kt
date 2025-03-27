package ui.navigation

import appLink
import at.asitplus.catchingUnwrapped
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import at.asitplus.valera.resources.biometric_authentication_prompt_to_bind_credentials_title
import at.asitplus.valera.resources.snackbar_credential_loaded_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.NavigationService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.SigningService
import at.asitplus.wallet.app.common.SigningState
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase
import domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import io.github.aakira.napier.Napier
import io.ktor.http.parseQueryString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import ui.navigation.routes.HomeScreenRoute
import ui.navigation.routes.LoadingRoute

class IntentService(
    val cryptoService: WalletCryptoService,
    val provisioningService: ProvisioningService,
    val presentationService: PresentationService,
    val snackbarService: SnackbarService,
    val errorService: ErrorService,
    val platformAdapter: PlatformAdapter,
    val signingService: SigningService,
    val navigationService: NavigationService) {

    val readyForIntents = MutableStateFlow<Boolean?>(null)

    suspend fun handleIntent(uri: String) {
        Napier.d("New intent: $uri")
        when (parseIntent(uri)) {
            IntentType.ProvisioningIntent -> {
                navigationService.navigate(LoadingRoute)

                catchingUnwrapped{
                    cryptoService.promptText =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_title)
                    cryptoService.promptSubtitle =
                        getString(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle)
                    provisioningService.resumeWithAuthCode(uri)
                    snackbarService.showSnackbar(getString(Res.string.snackbar_credential_loaded_successfully))
                    navigationService.navigateBack()
                    appLink.value = null
                }.onFailure {
                    navigationService.navigateBack()
                    errorService.emit(it)
                    appLink.value = null
                }
            }

            IntentType.ErrorIntent -> {
                val parameterIndex = uri.indexOfFirst { it == '?' }
                val pars = parseQueryString(uri, startIndex = parameterIndex + 1)
                runBlocking {
                    errorService.emit(
                        Exception(pars["error_description"] ?: "Unknown Exception")
                    )
                }
                appLink.value = null
            }

            IntentType.AuthorizationIntent -> {
                val consentPageBuilder =
                    BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase(
                        presentationService = presentationService
                    )

                consentPageBuilder(uri).unwrap().onSuccess {
                    Napier.d("valid authentication request")
                    navigationService.navigate(it)
                }.onFailure {
                    Napier.d("invalid authentication request")
                }
                appLink.value = null
            }

            IntentType.DCAPIAuthorizationIntent -> {
                val dcApiRequest = platformAdapter.getCurrentDCAPIData()
                val consentPageBuilder =
                    BuildAuthenticationConsentPageFromAuthenticationRequestDCAPIUseCase()


                consentPageBuilder(dcApiRequest).unwrap().onSuccess {
                    Napier.d("valid authentication request")
                    navigationService.navigate(it)
                }.onFailure {
                    errorService.emit(Exception("Invalid Authentication Request"))
                }

                appLink.value = null
            }

            IntentType.SigningServiceIntent -> {
                catchingUnwrapped {
                    signingService.resumeWithServiceAuthCode(url = uri)
                }.onSuccess {
                    navigationService.navigate(HomeScreenRoute)
                }.onFailure { e ->
                    errorService.emit(e)
                }
            }
            IntentType.SigningPreloadIntent -> {
                catchingUnwrapped {
                    signingService.resumePreloadCertificate(url = uri)
                }.onFailure { e ->
                    errorService.emit(e)
                }
            }
            IntentType.SigningCredentialIntent -> {
                catchingUnwrapped {
                    signingService.resumeWithCredentialAuthCode(url = uri)
                }.onSuccess {
                    navigationService.navigate(HomeScreenRoute)
                }.onFailure { e ->
                    errorService.emit(e)
                }
            }
            IntentType.SigningIntent -> {
                catchingUnwrapped {
                    signingService.start(uri)
                }.onFailure { e ->
                    errorService.emit(e)
                }
            }
        }
    }

    fun parseIntent(uri: String): IntentType {
        return if (uri.contains("error")) {
            IntentType.ErrorIntent
        } else if((signingService.redirectUri?.let { uri.contains(it) } == true)) {
            when (signingService.state) {
                SigningState.ServiceRequest -> IntentType.SigningServiceIntent
                SigningState.CredentialRequest -> IntentType.SigningCredentialIntent
                SigningState.PreloadCredential -> IntentType.SigningPreloadIntent
                null -> throw Throwable("Missing state in SigningService")
            }.also { signingService.state = null }
        } else if (provisioningService.redirectUri?.let { uri.contains(it) } == true) {
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