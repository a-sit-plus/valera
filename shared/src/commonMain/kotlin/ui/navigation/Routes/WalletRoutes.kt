package ui.navigation.Routes

import data.storage.StoreEntryId
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute : Route()

@Serializable
object AddCredentialRoute : Route()

@Serializable
class LoadCredentialRoute(val host: String) : Route()

@Serializable
data class AddCredentialPreAuthnRoute(val credentialOfferInfoSerialized: String) : Route()

@Serializable
data class CredentialDetailsRoute(val storeEntryId: StoreEntryId) : Route()

@Serializable
data class ProvisioningLoadingRoute(val link: String) : Route()

@Serializable
object SettingsRoute : Route()

@Serializable
object PreAuthQrCodeScannerRoute : Route()

@Serializable
object LogRoute : Route()

@Serializable
data class ErrorRoute(val message: String?, val cause: String?) : Route()

@Serializable
object LoadingRoute : Route()

@Serializable
object AuthenticationQrCodeScannerRoute : Route()

@Serializable
object AuthenticationLoadingRoute : Route()

@Serializable
data class AuthenticationConsentRoute(
    val authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    val authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    val recipientLocation: String
) : Route()

@Serializable
object AuthenticationSuccessRoute : Route()