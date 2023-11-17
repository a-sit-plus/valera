import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import view.ShowIdHeader

// Modified from https://developer.android.com/jetpack/compose/testing
class InstrumentedTests {

    @get:Rule
    val composeTestRule = createComposeRule()
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity

    @Test
    fun test1() {
        // Start the app
        composeTestRule.setContent {
            ShowIdHeader()
        }
        composeTestRule.onNodeWithText(Resources.WALLET).assertIsDisplayed()
    }
}