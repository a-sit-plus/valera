package ui.navigation.routes

import data.storage.StoreEntryId
import kotlinx.serialization.Serializable
import ui.viewmodels.QrCodeScannerMode

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
object LogRoute : Route()

@Serializable
data class SigningQtspSelectionRoute(val signatureRequestParametersSerialized: String) : Route()

@Serializable
data class ErrorRoute(val message: String?, val cause: String?) : Route()

@Serializable
object LoadingRoute : Route()

@Serializable
object PresentDataRoute : Route()

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
data class AuthenticationSuccessRoute(
    val redirectUrl: String?,
    val isCrossDeviceFlow: Boolean
) : Route()

@Serializable
object ShowQrCodeRoute : Route()

@Serializable
object VerifyDataRoute : Route()

@Serializable
data class ProvisioningIntentRoute(val uri: String) : Route()

@Serializable
data class AuthorizationIntentRoute(val uri: String) : Route()

@Serializable
data class DCAPIAuthorizationIntentRoute(val uri: String) : Route()

@Serializable
data class PresentationIntentRoute(val uri: String) : Route()

@Serializable
data class SigningServiceIntentRoute(val uri: String) : Route()

@Serializable
data class SigningPreloadIntentRoute(val uri: String) : Route()

@Serializable
data class SigningCredentialIntentRoute(val uri: String) : Route()

@Serializable
data class SigningIntentRoute(val uri: String) : Route()

@Serializable
data class ErrorIntentRoute(val uri: String) : Route()

@Serializable
data class QrCodeScannerRoute(val mode: QrCodeScannerMode) : Route()
