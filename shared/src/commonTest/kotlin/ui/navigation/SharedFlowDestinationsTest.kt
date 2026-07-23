package ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedFlowDestinationsTest {
    @Test
    fun androidTransientDcApiFlowShowsStartRoute() {
        assertTrue(shouldShowDcApiStartRoute(SharedDestinationFlow.Transient, "Android"))
    }

    @Test
    fun iosTransientDcApiFlowSkipsStartRoute() {
        assertFalse(shouldShowDcApiStartRoute(SharedDestinationFlow.Transient, "iOS"))
    }

    @Test
    fun walletFlowAlwaysShowsStartRoute() {
        assertTrue(shouldShowDcApiStartRoute(SharedDestinationFlow.Wallet, "Android"))
        assertTrue(shouldShowDcApiStartRoute(SharedDestinationFlow.Wallet, "iOS"))
    }
}
