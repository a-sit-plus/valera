
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

// Modified from https://developer.android.com/jetpack/compose/testing
class InstrumentedTests {

    @get:Rule
    val composeTestRule = createComposeRule()
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity


// test does not really apply anymore, the app starts with an empty screen until data store has been accessed
//    @Test
//    fun test1() {
//        // Start the app
//        composeTestRule.setContent {
//            val walletMain = WalletMain(objectFactory = AndroidObjectFactory(), dataStoreService = DummyDataStoreService(), platformAdapter = DummyPlatformAdapter())
//            App(walletMain)
//            runBlocking {
//                walletMain.resetApp()
//            }
//        }
//        composeTestRule.onNodeWithText(Resources.BUTTON_LABEL_START).assertIsDisplayed()
//    }
}