package ui.navigation

import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.SigningService
import at.asitplus.wallet.app.common.SigningState
import kotlinx.coroutines.flow.MutableStateFlow
import ui.navigation.routes.AuthorizationIntentRoute
import ui.navigation.routes.DCAPIAuthorizationIntentRoute
import ui.navigation.routes.ErrorIntentRoute
import ui.navigation.routes.PresentationIntentRoute
import ui.navigation.routes.ProvisioningIntentRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SigningCredentialIntentRoute
import ui.navigation.routes.SigningIntentRoute
import ui.navigation.routes.SigningPreloadIntentRoute
import ui.navigation.routes.SigningServiceIntentRoute

const val PRESENTATION_REQUESTED_INTENT = "PRESENTATION_REQUESTED"
const val GET_CREDENTIALS_INTENT = "androidx.identitycredentials.action.GET_CREDENTIALS"

class IntentService(
    val provisioningService: ProvisioningService,
    val signingService: SigningService
) {
    val readyForIntents = MutableStateFlow<Boolean?>(null)

    fun handleIntent(uri: String): Route =
        when (parseUrl(uri)) {
            IntentType.ProvisioningIntent -> ProvisioningIntentRoute(uri)
            IntentType.AuthorizationIntent -> AuthorizationIntentRoute(uri)
            IntentType.DCAPIAuthorizationIntent -> DCAPIAuthorizationIntentRoute(uri)
            IntentType.PresentationIntent -> PresentationIntentRoute(uri)
            IntentType.SigningServiceIntent -> SigningServiceIntentRoute(uri)
            IntentType.SigningPreloadIntent -> SigningPreloadIntentRoute(uri)
            IntentType.SigningCredentialIntent -> SigningCredentialIntentRoute(uri)
            IntentType.SigningIntent -> SigningIntentRoute(uri)
            IntentType.ErrorIntent -> ErrorIntentRoute(uri)
        }


    fun parseUrl(url: String): IntentType {
        return if (url.contains("error")) {
            IntentType.ErrorIntent
        } else if ((signingService.redirectUri?.let { url.contains(it) } == true)) {
            when (signingService.state) {
                SigningState.ServiceRequest -> IntentType.SigningServiceIntent
                SigningState.CredentialRequest -> IntentType.SigningCredentialIntent
                SigningState.PreloadCredential -> IntentType.SigningPreloadIntent
                null -> throw Throwable("Missing state in SigningService")
            }.also { signingService.state = null }
        } else if (provisioningService.redirectUri?.let { url.contains(it) } == true) {
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

    enum class IntentType {
        ErrorIntent,
        ProvisioningIntent,
        AuthorizationIntent,
        DCAPIAuthorizationIntent,
        PresentationIntent,
        SigningServiceIntent,
        SigningCredentialIntent,
        SigningPreloadIntent,
        SigningIntent,
    }
}