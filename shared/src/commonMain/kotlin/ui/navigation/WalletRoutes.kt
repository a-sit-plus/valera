package ui.navigation

import Route
import data.storage.StoreEntryId
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenRoute : Route()

@Serializable
object AddCredentialRoute : Route()

@Serializable
data class AddCredentialPreAuthnRoute (val credentialOfferInfoSerialized: String) : Route()

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
object LoadingRoute : Route()

@Serializable
object AuthenticationQrCodeScannerRoute : Route()

@Serializable
object AuthenticationLoadingRoute : Route()

@Serializable
data class AuthenticationConsentRoute(
    val authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    val authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    val recipientLocation: String) : Route()

@Serializable
object AuthenticationSuccessRoute : Route()