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
data class ErrorRoute(val message: String?, val cause: String?) : Route()

@Serializable
object LoadingRoute : Route()

@Serializable
object AuthenticationQrCodeScannerRoute : Route()

@Serializable
data class AuthenticationViewRoute(
    val authenticationRequestParametersFromSerialized: String, // AuthenticationRequest
    val authorizationPreparationStateSerialized: String, // AuthenticationResultParameters
    val recipientLocation: String
) : Route()

@Serializable
object AuthenticationSuccessRoute : Route()

@Serializable
object AuthenticationSuccessPage : Route()

@Serializable
object SelectDataRetrievalPage : Route()

@Serializable
object CustomDataRetrievalPage : Route()

@Serializable
data class QrDeviceEngagementPage (
    val documentSerialized: String
) : Route()

@Serializable
data class LoadRequestedDataPage(
    val documentSerialized: String,
    val payload: String
): Route()

@Serializable
object RequestedDataLogOutputPage : Route()

@Serializable
object RequestedDataShowPage: Route()