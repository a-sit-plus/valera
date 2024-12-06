import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_accept
import compose_wallet_app.shared.generated.resources.button_label_consent
import compose_wallet_app.shared.generated.resources.button_label_continue
import compose_wallet_app.shared.generated.resources.button_label_details
import compose_wallet_app.shared.generated.resources.button_label_show_data
import compose_wallet_app.shared.generated.resources.button_label_start
import compose_wallet_app.shared.generated.resources.content_description_navigate_back
import compose_wallet_app.shared.generated.resources.content_description_portrait
import compose_wallet_app.shared.generated.resources.heading_label_credential_details_screen
import compose_wallet_app.shared.generated.resources.section_heading_actions
import compose_wallet_app.shared.generated.resources.section_heading_age_data
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString
import ui.navigation.Routes.OnboardingWrapperTestTags
import ui.views.OnboardingStartScreenTestTag
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.stringResource
import ui.navigation.NavigatorTestTags
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertTrue


// Modified from https://developer.android.com/jetpack/compose/testing
@OptIn(ExperimentalTestApi::class)
class InstrumentedTests {




/*

    @BeforeTest
    fun setup() = runTest {
        withContext(Dispatchers.Main) {
            lifecycleOwner = TestLifecycleOwner()
            lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    @AfterTest
    fun teardown() = runTest {
        withContext(Dispatchers.Main) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

 */






    @Test
    fun givenNewAppInstallation_whenStartingApp_thenAppActuallyStarts() = runComposeUiTest() {

        // Start the app
        setContent {
            val dummyDataStoreService = DummyDataStoreService()
            val ks = KeystoreService(dummyDataStoreService)
            val walletMain = WalletMain(
                cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
                holderKeyService = ks,
                dataStoreService = dummyDataStoreService,
                platformAdapter = getPlatformAdapter(),
                scope =  CoroutineScope(Dispatchers.Default),
                buildContext = BuildContext(
                    buildType = "debug",
                    versionCode = 0,
                    versionName = "0.0.0",
                )
            )
            App(walletMain)
        }

        onNodeWithTag(AppTestTags.rootScaffold)
            .assertIsDisplayed()
    }


    @Test
    fun givenNewAppInstallation_whenStartingApp_thenShowsOnboardingStartScreen() = runComposeUiTest() {
        // Start the app
        setContent {
            val dummyDataStoreService = DummyDataStoreService()
            val ks = KeystoreService(dummyDataStoreService)
            val walletMain = WalletMain(
                cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
                holderKeyService = ks,
                dataStoreService = dummyDataStoreService,
                platformAdapter = getPlatformAdapter(),
                scope =  CoroutineScope(Dispatchers.Default),
                buildContext = BuildContext(
                    buildType = "debug",
                    versionCode = 0,
                    versionName = "0.0.0",
                )
            )
            App(walletMain)
        }

        waitUntil {
            onNodeWithTag(NavigatorTestTags.loadingTestTag)
                .isNotDisplayed()
        }

        onNodeWithTag(OnboardingWrapperTestTags.onboardingStartScreen)
            .assertIsDisplayed()


    }

    @Test
    fun givenNewAppInstallation_whenStartingApp_thenShowsOnboardingStartButton() = runComposeUiTest() {
        // Start the app
        setContent {
            val dummyDataStoreService = DummyDataStoreService()
            val ks = KeystoreService(dummyDataStoreService)
            val walletMain = WalletMain(
                cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
                holderKeyService = ks,
                dataStoreService = dummyDataStoreService,
                platformAdapter = getPlatformAdapter(),
                scope =  CoroutineScope(Dispatchers.Default),
                buildContext = BuildContext(
                    buildType = "debug",
                    versionCode = 0,
                    versionName = "0.0.0",
                )
            )
            App(walletMain)
        }

        waitUntil {
            onNodeWithTag(NavigatorTestTags.loadingTestTag)
                .isNotDisplayed()
        }

        waitUntil {
            onNodeWithTag(OnboardingWrapperTestTags.onboardingStartScreen)
                .isDisplayed()
        }

        onNodeWithTag(OnboardingStartScreenTestTag.startButton)
            .assertIsDisplayed()


    }




    /*
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun givenNewAppInstallation_whenStartingApp_thenLoadAttributesAndShowData() = runComposeUiTest() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val dummyDataStoreService = DummyDataStoreService()
                val ks = KeystoreService(dummyDataStoreService)
                val walletMain = WalletMain(
                    cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
                    holderKeyService = ks,
                    dataStoreService = dummyDataStoreService,
                    platformAdapter = getPlatformAdapter(),
                    scope = CoroutineScope(Dispatchers.Default),
                    subjectCredentialStore = PersistentSubjectCredentialStore(dummyDataStoreService),
                    buildContext = BuildContext(
                        buildType = "debug",
                        versionCode = 0,
                        versionName = "0.0.0",
                    )
                )
                App(walletMain)

                val issuer = IssuerAgent()

                runBlocking {
                    walletMain.holderAgent.storeCredential(
                        issuer.issueCredential(
                            CredentialToBeIssued.VcSd(
                                getAttributes(),
                                Clock.System.now().plus(3600.minutes),
                                IdAustriaScheme,
                                walletMain.cryptoService.keyMaterial.publicKey,
                            )
                        ).getOrThrow().toStoreCredentialInput()
                    )
                }
            }
        }
        runBlocking {

            onNodeWithText(getString(Res.string.button_label_start))
                .assertIsDisplayed()
            onNodeWithText(getString(Res.string.button_label_start)).performClick()
            onNodeWithText(getString(Res.string.button_label_continue))
                .assertIsDisplayed()
            onNodeWithText(getString(Res.string.button_label_continue)).performClick()
            onNodeWithText(getString(Res.string.button_label_accept))
                .assertIsDisplayed()
            onNodeWithText(getString(Res.string.button_label_accept)).performClick()
            waitUntilDoesNotExist(hasText(getString(Res.string.button_label_accept)), 2000)

            val client = HttpClient() {
                expectSuccess = true
                install(ContentNegotiation) {
                    json()
                }
            }


            val responseGenerateRequest =
                client.post("https://apps.egiz.gv.at/customverifier/transaction/create") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<JsonObject>()

            val qrCodeUrl = responseGenerateRequest["qrCodeUrl"]?.jsonPrimitive?.content
            val id = responseGenerateRequest["id"]?.jsonPrimitive?.content

            appLink.value = qrCodeUrl

            waitUntilExactlyOneExists(
                hasText(getString(Res.string.button_label_consent)),
                10000
            )

            onNodeWithText(getString(Res.string.button_label_consent)).performClick()

            val url = "https://apps.egiz.gv.at/customverifier/customer-success.html?id=$id"
            val responseSuccess = client.get(url)
            assertTrue { responseSuccess.status.value in 200..299 }
            //lifecycleRegistry.currentState= Lifecycle.State.DESTROYED
        }

    }



     */
}

val request = Json.encodeToString(RequestBody(
    "https://wallet.a-sit.at/mobile",
    listOf(Credential(
        "at.gv.id-austria.2023.1",
        "SD_JWT",
        listOf(
            "bpk",
            "firstname",
            "lastname",
            "date-of-birth",
            "portrait",
            "main-address",
            "age-over-18",
        )
    ))
))




@Serializable
data class RequestBody(val urlprefix: String, val credentials: List<Credential>)

@Serializable
data class Credential(val credentialType: String, val representation: String, val attributes: List<String>)

@Composable
expect fun getPlatformAdapter(): PlatformAdapter
