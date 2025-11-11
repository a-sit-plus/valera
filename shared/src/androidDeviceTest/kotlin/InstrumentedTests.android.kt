package at.asitplus.wallet.app

import AndroidPlatformAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import at.asitplus.wallet.app.common.PlatformAdapter
import de.infix.testBalloon.framework.core.testSuite


@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    val context = LocalContext.current
    return AndroidPlatformAdapter(context)
}

@OptIn(ExperimentalTestApi::class)
val AndroidComposeUiTest by testSuite {
    test("EndToEnd") {
        runComposeUiTest {
            endToEndTest()
        }
    }
}