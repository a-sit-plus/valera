package ui.navigation

import at.asitplus.wallet.app.common.PlatformAdapter
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
const val SIGNING_REQUEST_INTENT = "createSignRequest"
const val GET_CREDENTIALS_INTENT = "androidx.identitycredentials.action.GET_CREDENTIALS"
const val GET_CREDENTIAL_INTENT = "androidx.credentials.registry.provider.action.GET_CREDENTIAL"

class IntentService(
    val platformAdapter: PlatformAdapter
) {
    var redirectUri: String? = null
    var intentType: IntentType? = null

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

    fun parseUrl(url: String): IntentType = with(url) {
        when {
            contains("error") -> IntentType.ErrorIntent
            contains(SIGNING_REQUEST_INTENT) -> IntentType.SigningIntent
            equals(GET_CREDENTIALS_INTENT) || equals(GET_CREDENTIAL_INTENT) -> IntentType.DCAPIAuthorizationIntent
            equals(PRESENTATION_REQUESTED_INTENT) -> IntentType.PresentationIntent
            contains("request_uri") && contains("client_id") -> IntentType.AuthorizationIntent
            (redirectUri != null && contains(redirectUri!!) && intentType != null) -> intentType!!
            else -> IntentType.AuthorizationIntent
        }
    }

    fun openIntent(url: String, redirectUri: String? = null, intentType: IntentType? = null) {
        this.redirectUri = redirectUri
        this.intentType = intentType
        platformAdapter.openUrl(url)
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