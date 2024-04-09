import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import at.asitplus.KmmResult
import at.asitplus.wallet.app.android.MainActivity
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.iso.IssuerSigned
import composewalletapp.shared.generated.resources.button_label_start
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_accept
import composewalletapp.shared.generated.resources.button_label_continue
import composewalletapp.shared.generated.resources.button_label_load_data
import composewalletapp.shared.generated.resources.button_label_reload_data
import composewalletapp.shared.generated.resources.navigation_button_label_show_data
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import ui.navigation.AuthenticationConsentPage
import view.AuthenticationQrCodeScannerViewModel

// Modified from https://developer.android.com/jetpack/compose/testing
@OptIn(ExperimentalResourceApi::class)
class InstrumentedTests {

    @get:Rule
    //val composeTestRule = createComposeRule()
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    //
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity


    @OptIn(ExperimentalTestApi::class)
    @Test
    fun givenNewAppInstallation_whenStartingApp_thenShowsOnboardingStartScreen() {
        // Start the app
        val walletMain = WalletMain(
            //testRun = true,
            //neue objectFactory, mit Keystoreservie ohne biometrie
            objectFactory = AndroidObjectFactory(),
            dataStoreService = DummyDataStoreService(),
            platformAdapter = DummyPlatformAdapter(),
            buildContext = BuildContext(
                buildType = "debug",
                versionCode = 0,
                versionName = "0.0.0",
            ),
            //for test data
            subjectCredentialStore = DummyPersistentSubjectCredentialStore()
        )
        composeTestRule.setContent {
            App(walletMain)
        }
        runBlocking {
            walletMain.platformAdapter.openUrl("")
            composeTestRule.activity.intent.
            composeTestRule.onNodeWithText(getString(Res.string.button_label_start))
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(getString(Res.string.button_label_start)).performClick()
            composeTestRule.onNodeWithText(getString(Res.string.button_label_continue))
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(getString(Res.string.button_label_continue)).performClick()
            composeTestRule.onNodeWithText(getString(Res.string.button_label_accept))
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(getString(Res.string.button_label_accept)).performClick()
            composeTestRule.onNodeWithText(getString(Res.string.navigation_button_label_show_data))
                .assertIsDisplayed()
            composeTestRule.onNodeWithText(getString(Res.string.navigation_button_label_show_data)).performClick()
            //composeTestRule.onNodeWithText("allow")
              //  .assertIsDisplayed()
           // composeTestRule.onNodeWithText("allow").performClick()

            composeTestRule.waitUntilDoesNotExist(hasText("Loading"), 10000)
            composeTestRule.onNodeWithText(getString(Res.string.button_label_reload_data)).performClick()
            composeTestRule.waitUntilDoesNotExist(hasText(getString(Res.string.button_label_reload_data)), 10000)
            composeTestRule.waitUntilAtLeastOneExists(hasText(getString(Res.string.button_label_load_data)), 10000)
            composeTestRule.onNode(
                hasText(getString(Res.string.button_label_load_data))
                and
                hasClickAction()
            ).performClick()

            composeTestRule.waitUntilDoesNotExist(hasText(getString(Res.string.button_label_load_data)), 10000)
            Thread.sleep(10000)

            onNewIntent()
        }
    }
}

class DummyPersistentSubjectCredentialStore : SubjectCredentialStore {
    override suspend fun getAttachment(name: String): KmmResult<ByteArray> {
        TODO("Not yet implemented")
    }

    override suspend fun getAttachment(name: String, vcId: String): KmmResult<ByteArray> {
        TODO("Not yet implemented")
    }

    //
    override suspend fun getCredentials(credentialSchemes: Collection<ConstantIndex.CredentialScheme>?): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        TODO("Not yet implemented")
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme
    ) {
        TODO("Not yet implemented")
    }

}
