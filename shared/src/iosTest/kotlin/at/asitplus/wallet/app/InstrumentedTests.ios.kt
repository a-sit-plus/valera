package at.asitplus.wallet.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.PlatformAdapter
import kotlin.test.Test

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    return DummyPlatformAdapter()
}

@OptIn(ExperimentalTestApi::class)
@Test
fun IosComposeUiTest() = runComposeUiTest {
    endToEndTest()
}