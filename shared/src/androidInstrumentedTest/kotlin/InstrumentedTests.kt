
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import data.storage.DummyDataStoreService
import org.junit.Rule
import org.junit.Test

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
            App(
                WalletMain(objectFactory = AndroidObjectFactory(), realDataStoreService = DummyDataStoreService(), platformAdapter = DummyPlatformAdapter())
            )
        }
        composeTestRule.onNodeWithText(Resources.WALLET).assertIsDisplayed()
    }
}