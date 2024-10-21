package ui.navigation

import Route
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute : Route

@Serializable
object AddCredentialRoute : Route

@Serializable
object AddCredentialPreAuthnRoute : Route

@Serializable
object CredentialDetailsRoute : Route

@Serializable
object ProvisioningLoadingRoute : Route

@Serializable
object SettingsRoute : Route

@Serializable
object PreAuthQrCodeScannerRoute : Route

@Serializable
object LogRoute : Route

@Serializable
object LoadingRoute : Route

@Serializable
object AuthenticationQrCodeScannerRoute : Route

@Serializable
object AuthenticationLoadingRoute : Route

@Serializable
data class AuthenticationConsentRoute(
    val authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    val authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    val recipientLocation: String) : Route

@Serializable
object AuthenticationSuccessRoute : Route