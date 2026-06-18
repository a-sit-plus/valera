package ui.navigation.routes

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
}
