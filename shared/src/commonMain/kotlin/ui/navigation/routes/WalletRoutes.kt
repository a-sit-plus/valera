package ui.navigation.routes

import data.storage.StoreEntryId
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute : Route()

@Serializable
object AddCredentialRoute : Route()

@Serializable
class LoadCredentialRoute(val host: String) : Route()

@Serializable
data class AddCredentialPreAuthnRoute(val credentialOfferSerialized: String) : Route()

@Serializable
data class CredentialDetailsRoute(val storeEntryId: StoreEntryId) : Route()

@Serializable
object SettingsRoute : Route()

@Serializable
object PreAuthQrCodeScannerRoute : Route()

@Serializable
object LogRoute : Route()

@Serializable
object SigningRoute: Route()

@Serializable
object SigningQtspSelectionRoute: Route()

@Serializable
data class ErrorRoute(val message: String?, val cause: String?) : Route()

@Serializable
object LoadingRoute : Route()

@Serializable
object AuthenticationQrCodeScannerRoute : Route()

@Serializable
data class AuthenticationViewRoute(
    val authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    val authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    val recipientLocation: String,
    val isCrossDeviceFlow: Boolean,
) : Route()

@Serializable
data class DCAPIAuthenticationConsentRoute(
    val apiRequestSerialized: String
) : Route()

@Serializable
data class LocalPresentationAuthenticationConsentRoute(
    val presentationRequestSerialized: String
) : Route()

@Serializable
object AuthenticationSuccessRoute : Route()