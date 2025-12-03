package ui.navigation.routes

import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.CredentialOffer
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.SignatureRequestParameters
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.presentation.PresentationRequest
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import data.storage.StoreEntryId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ui.viewmodels.QrCodeScannerMode

@Serializable
open class Route()

@Serializable
open class PrerequisiteRoute(val prerequisitesSerialized: String) : Route() {
    constructor(
        prerequisites: Set<RoutePrerequisites>
    ) : this(
        joseCompliantSerializer.encodeToString(prerequisites)
    )

    val prerequisites: Set<RoutePrerequisites>
        get() = joseCompliantSerializer.decodeFromString(prerequisitesSerialized)
}

@Serializable
enum class RoutePrerequisites {
    CRYPTO,
    INTERNET,
    CAMERA
}

@Serializable
object InitializationRoute : Route()

@Serializable
object HomeScreenRoute : PrerequisiteRoute(setOf(RoutePrerequisites.CRYPTO))

@Serializable
object AddCredentialRoute : PrerequisiteRoute(
    setOf(
        RoutePrerequisites.INTERNET,
        RoutePrerequisites.CRYPTO
    )
)

@Serializable
class LoadCredentialRoute(val host: String) : Route()

@Serializable
data class AddCredentialPreAuthnRoute(
    val credentialOfferSerialized: String
) : Route(
) {
    constructor(
        credentialOffer: CredentialOffer
    ) : this(
        joseCompliantSerializer.encodeToString(credentialOffer)
    )

    val credentialOffer: CredentialOffer
        get() = joseCompliantSerializer.decodeFromString(credentialOfferSerialized)
}

@Serializable
data class AddCredentialWithLinkRoute(val uri: String) : Route()

@Serializable
data class CredentialDetailsRoute(val storeEntryId: StoreEntryId) : Route()

@Serializable
object SettingsRoute : Route()

@Serializable
object LogRoute : Route()

@Serializable
data class SigningQtspSelectionRoute(
    val signatureRequestParametersSerialized: String
) : Route() {
    constructor(
        signatureRequestParameters: SignatureRequestParameters
    ) : this(
        vckJsonSerializer.encodeToString(signatureRequestParameters)
    )

    val signatureRequestParameters: SignatureRequestParameters
        get() = vckJsonSerializer.decodeFromString(signatureRequestParametersSerialized)
}

@Serializable
object ErrorRoute : Route()

@Serializable
object LoadingRoute : Route()

@Serializable
object PresentDataRoute : Route()

@Serializable
data class AuthenticationViewRoute(
    val authenticationRequestParametersFromSerialized: String,
    val authorizationPreparationStateSerialized: String,
    val recipientLocation: String,
    val isCrossDeviceFlow: Boolean,
) : Route() {
    constructor(
        authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>,
        authorizationResponsePreparationState: AuthorizationResponsePreparationState,
        recipientLocation: String,
        isCrossDeviceFlow: Boolean,
    ) : this(
        authenticationRequestParametersFromSerialized = vckJsonSerializer.encodeToString(authenticationRequest),
        authorizationPreparationStateSerialized = vckJsonSerializer.encodeToString(authorizationResponsePreparationState),
        recipientLocation = recipientLocation,
        isCrossDeviceFlow
    )

    val authenticationRequest: RequestParametersFrom<AuthenticationRequestParameters>
        get() = vckJsonSerializer.decodeFromString(authenticationRequestParametersFromSerialized)
    val authorizationResponsePreparationState: AuthorizationResponsePreparationState
        get() = vckJsonSerializer.decodeFromString(authorizationPreparationStateSerialized)
}

@Serializable
data class DCAPIAuthenticationConsentRoute(
    val apiRequestSerialized: String
) : Route()

@Serializable
data class LocalPresentationAuthenticationConsentRoute(
    val presentationRequestSerialized: String
) : Route() {
    constructor(presentationRequest: PresentationRequest) : this(
        vckJsonSerializer.encodeToString(presentationRequest)
    )
}

@Serializable
data class AuthenticationSuccessRoute(
    val redirectUrl: String?,
    val isCrossDeviceFlow: Boolean
) : Route()

@Serializable
object ProximityHolderRoute : Route()

@Serializable
object ProximityVerifierRoute : Route()

@Serializable
data class ProvisioningStartIntentRoute(val uri: String) : Route()

@Serializable
data class ProvisioningResumeIntentRoute(val uri: String) : Route()

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
data class QrCodeScannerRoute(val modeSerialized: String) : PrerequisiteRoute(
    setOf(
        RoutePrerequisites.CAMERA
    )
) {
    constructor(
        mode: QrCodeScannerMode
    ) : this(
        vckJsonSerializer.encodeToString(mode)
    )

    val mode: QrCodeScannerMode
        get() = vckJsonSerializer.decodeFromString(modeSerialized)
}

@Serializable
data class CapabilitiesRoute(val prerequisitesSerialized: String) : Route() {
    constructor(
        prerequisites: Set<RoutePrerequisites>,
    ) : this(
        vckJsonSerializer.encodeToString(prerequisites)
    )

    val prerequisites: Set<RoutePrerequisites>
        get() = vckJsonSerializer.decodeFromString(prerequisitesSerialized)
}
