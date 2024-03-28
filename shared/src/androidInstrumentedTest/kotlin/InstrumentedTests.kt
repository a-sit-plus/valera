
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import data.storage.DummyDataStoreService
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.junit.Rule
import org.junit.Test

// Modified from https://developer.android.com/jetpack/compose/testing
@OptIn(ExperimentalResourceApi::class)
class InstrumentedTests {

    @get:Rule
    val composeTestRule = createComposeRule()
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity


    @Test
    fun givenNewAppInstallation_whenStartingApp_thenShowsOnboardingStartScreen() {
        // Start the app
        composeTestRule.setContent {
            val walletMain = WalletMain(
                objectFactory = AndroidObjectFactory(),
                dataStoreService = DummyDataStoreService(),
                platformAdapter = DummyPlatformAdapter(),
                buildContext = BuildContext(
                    buildType = "debug",
                    versionCode = 0,
                    versionName = "0.0.0",
                )
            )
            App(walletMain)
        }

        composeTestRule.onNodeWithTag("root")
            .assertIsDisplayed()
    }
}