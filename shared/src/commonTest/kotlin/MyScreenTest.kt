import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import at.asitplus.wallet.app.common.MainApplication
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainScreenTest {

    @Test
    fun myFirstTest() = runComposeUiTest {
        setContent {
            MainApplication()
        }
        // 1)
        onNodeWithText("You have pushed the button this many times").assertExists()
        // 2)
        onNodeWithText("0").assertExists()
        // 3)
        onNodeWithContentDescription("Increment").performClick()
        // 4)
        onNodeWithText("1").assertExists()

    }
}

