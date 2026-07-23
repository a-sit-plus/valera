package ui.navigation.routes

import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.LoadingMessageKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ui.viewmodels.QrCodeScannerMode

class WalletRoutesTest {
    @Test
    fun qrCredentialOfferRouteRequiresSameCapabilitiesAsAddCredentialRoute() {
        val route: Route = AddCredentialPreAuthnRoute("serialized-offer")

        assertTrue(route is PrerequisiteRoute)
        assertEquals(AddCredentialRoute.prerequisites, route.prerequisites)
    }

    @Test
    fun dcApiCredentialOfferRouteRequiresSameCapabilitiesAsAddCredentialRoute() {
        val route: Route = AddCredentialDcApiRoute("serialized-offer")

        assertTrue(route is PrerequisiteRoute)
        assertEquals(AddCredentialRoute.prerequisites, route.prerequisites)
    }

    @Test
    fun qrScannerRoutesRequireCredentialLoadingCapabilitiesAndCamera() {
        val expectedPrerequisites = AddCredentialRoute.prerequisites + RoutePrerequisites.CAMERA

        QrCodeScannerMode.entries.forEach { mode ->
            assertEquals(
                expectedPrerequisites,
                QrCodeScannerRoute(mode).prerequisites,
            )
        }
    }

    @Test
    fun loadingRouteSerializesMessageKey() {
        val route = LoadingRoute(LoadingMessageKey.IssuerMetadata)

        val serialized = joseCompliantSerializer.encodeToString(route)
        val deserialized = joseCompliantSerializer.decodeFromString<LoadingRoute>(serialized)

        assertEquals(LoadingMessageKey.IssuerMetadata.name, deserialized.message)
        assertEquals(LoadingMessageKey.IssuerMetadata, deserialized.messageKey)
    }

    @Test
    fun dcApiPresentationRoutePreservesGenericRequest() {
        val parameters = AuthenticationRequestParameters(nonce = "test-nonce")
        val request = RequestParametersFrom.OpenId4VpDcApiUnsigned(
            parameters = parameters,
            jsonString = joseCompliantSerializer.encodeToString(parameters),
            credentialIds = listOf("test-credential"),
            callingPackageName = "com.example.verifier",
            callingOrigin = "https://verifier.example.com",
        )

        val route = DCAPIPresentationViewRoute(request)

        assertEquals(request, route.request)
    }
}
