package ui.navigation

import at.asitplus.wallet.app.common.PlatformAdapter
import ui.navigation.routes.AuthorizationIntentRoute
import ui.navigation.routes.DCAPIAuthorizationIntentRoute
import ui.navigation.routes.DCAPIIssuingIntentRoute
import ui.navigation.routes.ErrorIntentRoute
import ui.navigation.routes.IosDcApiPreRequestRoute
import ui.navigation.routes.PresentationIntentRoute
import ui.navigation.routes.ProvisioningStartIntentRoute
import ui.navigation.routes.ProvisioningResumeIntentRoute
import ui.navigation.routes.Route
import ui.navigation.routes.SigningCredentialIntentRoute
import ui.navigation.routes.SigningIntentRoute
import ui.navigation.routes.SigningPreloadIntentRoute
import ui.navigation.routes.SigningResumeIntentRoute
import ui.navigation.routes.SigningServiceIntentRoute

class IntentService(
    val platformAdapter: PlatformAdapter
) {
    var redirectUri: String? = null
    var intentType: IntentType? = null

    fun handleIntent(uri: String): Route = handleIntent(uri, parseUrl(uri))

    fun handleIntent(uri: String, type: IntentType): Route =
        when (type) {
            IntentType.ProvisioningStartIntent -> ProvisioningStartIntentRoute(uri)
            IntentType.ProvisioningResumeIntent -> ProvisioningResumeIntentRoute(uri)
            IntentType.AuthorizationIntent -> AuthorizationIntentRoute(uri)
            IntentType.DCAPIAuthorizationIntent -> DCAPIAuthorizationIntentRoute(uri)
            IntentType.IosDcApiPreRequestIntent -> IosDcApiPreRequestRoute
            IntentType.DCAPIIssuingIntent -> DCAPIIssuingIntentRoute(uri)
            IntentType.PresentationIntent -> PresentationIntentRoute(uri)
            IntentType.SigningServiceIntent -> SigningServiceIntentRoute(uri)
            IntentType.SigningPreloadIntent -> SigningPreloadIntentRoute(uri)
            IntentType.SigningCredentialIntent -> SigningCredentialIntentRoute(uri)
            IntentType.SigningIntent -> SigningIntentRoute(uri)
            IntentType.SigningResumeIntent -> SigningResumeIntentRoute(uri)
            IntentType.ErrorIntent -> ErrorIntentRoute(uri)
        }

    fun parseUrl(url: String): IntentType = with(url) {
        when {
            startsWith(SIGNING_CALLBACK_URI) -> IntentType.SigningResumeIntent
            startsWith(PROVISIONING_CALLBACK_URI) -> IntentType.ProvisioningResumeIntent
            contains(SIGNING_REQUEST_INTENT) -> IntentType.SigningIntent
            equals(GET_CREDENTIALS_INTENT) || equals(GET_CREDENTIAL_INTENT) || equals(IOS_DC_API_CALL) -> IntentType.DCAPIAuthorizationIntent
            equals(IOS_DC_API_PRE_REQUEST) -> IntentType.IosDcApiPreRequestIntent
            equals(CREATE_CREDENTIAL_INTENT) -> IntentType.DCAPIIssuingIntent
            equals(PRESENTATION_REQUESTED_INTENT) -> IntentType.PresentationIntent
            contains("request_uri") && contains("client_id") -> IntentType.AuthorizationIntent
            (redirectUri != null && contains(redirectUri!!) && intentType != null) -> intentType!!
            contains("credential_offer") || contains("credential_offer_uri") -> IntentType.ProvisioningStartIntent
            contains("error") -> IntentType.ErrorIntent
            else -> IntentType.AuthorizationIntent
        }
    }

    fun isContinuationIntent(type: IntentType): Boolean =
        type == IntentType.ProvisioningResumeIntent || type == IntentType.SigningResumeIntent

    fun openIntent(url: String, redirectUri: String? = null, intentType: IntentType? = null) {
        this.redirectUri = redirectUri
        this.intentType = intentType
        platformAdapter.openUrl(url)
    }

    companion object {
        const val PRESENTATION_REQUESTED_INTENT = "PRESENTATION_REQUESTED"
        const val SIGNING_REQUEST_INTENT = "createSignRequest"
        const val GET_CREDENTIALS_INTENT = "androidx.identitycredentials.action.GET_CREDENTIALS"
        const val GET_CREDENTIAL_INTENT = "androidx.credentials.registry.provider.action.GET_CREDENTIAL"
        const val CREATE_CREDENTIAL_INTENT = "androidx.credentials.registry.provider.action.CREATE_CREDENTIAL"
        const val IOS_DC_API_CALL = "IOS_DC_API_CALL"
        const val IOS_DC_API_PRE_REQUEST = "IOS_DC_API_PRE_REQUEST"
        const val SIGNING_CALLBACK_URI = "asitplus-wallet://wallet.a-sit.at/app/callback/signing"
        const val PROVISIONING_CALLBACK_URI = "asitplus-wallet://wallet.a-sit.at/app/callback/provisioning"
    }

    enum class IntentType {
        ErrorIntent,
        ProvisioningStartIntent,
        ProvisioningResumeIntent,
        AuthorizationIntent,
        DCAPIAuthorizationIntent,
        IosDcApiPreRequestIntent,
        DCAPIIssuingIntent,
        PresentationIntent,
        SigningServiceIntent,
        SigningCredentialIntent,
        SigningPreloadIntent,
        SigningIntent,
        SigningResumeIntent,
    }
}
