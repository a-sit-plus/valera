import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_accept
import at.asitplus.valera.resources.button_label_consent
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.button_label_details
import at.asitplus.valera.resources.button_label_start
import at.asitplus.valera.resources.content_description_portrait
import at.asitplus.valera.resources.section_heading_age_data
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import ui.navigation.NavigatorTestTags
import ui.navigation.Routes.OnboardingWrapperTestTags
import ui.views.OnboardingStartScreenTestTag
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

// Modified from https://developer.android.com/jetpack/compose/testing
@OptIn(ExperimentalTestApi::class)
class InstrumentedTests {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var lifecycleOwner: TestLifecycleOwner

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

    @Test
    fun givenNewAppInstallation_whenStartingApp_thenAppActuallyStarts() = runComposeUiTest() {

        // Start the app
        setContent {
            val dummyDataStoreService = DummyDataStoreService()
            val ks = KeystoreService(dummyDataStoreService)
            val walletMain = WalletMain(
                cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
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


    @Test
    fun givenNewAppInstallation_whenStartingApp_thenShowAttributesOnMyCredentialsScreen() = runComposeUiTest() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val dummyDataStoreService = DummyDataStoreService()
                val ks = KeystoreService(dummyDataStoreService)
                val walletMain = WalletMain(
                    cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
                    dataStoreService = dummyDataStoreService,
                    platformAdapter = getPlatformAdapter(),
                    scope = CoroutineScope(Dispatchers.Default),
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
                                walletMain.cryptoService.keyMaterial.publicKey
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
            waitUntilDoesNotExist(hasText(getString(Res.string.button_label_accept)), 10000)

            onNodeWithContentDescription(getString(Res.string.content_description_portrait)).assertHeightIsAtLeast(1.dp)
            onNodeWithText("XXXÉliás XXXTörőcsik").assertExists()
            onNodeWithText("11.10.1965").assertExists()

            onNodeWithText(getString(Res.string.button_label_details)).performClick()
            waitUntilExactlyOneExists(hasText(getString(Res.string.section_heading_age_data)))
            onNodeWithText("≥14").assertExists()
            onNodeWithText("≥16").assertExists()
            onNodeWithText("≥18").assertExists()
            onNodeWithText("≥21").assertExists()
            onNodeWithText("Testgasse 1a-2b/Stg. 3c-4d/D6").assertExists()
            onNodeWithText("0088 Testort A").assertExists()
        }
    }

    private fun getAttributes() : List<ClaimToBeIssued> {
        return listOf(
            ClaimToBeIssued(IdAustriaScheme.Attributes.BPK,"XFN+436920f:L9LBxmjNPt0041j5O1+sir0HOG0="),
            ClaimToBeIssued(IdAustriaScheme.Attributes.FIRSTNAME, "XXXÉliás"),
            ClaimToBeIssued(IdAustriaScheme.Attributes.LASTNAME, "XXXTörőcsik"),
            ClaimToBeIssued(IdAustriaScheme.Attributes.DATE_OF_BIRTH,"1965-10-11"),
            ClaimToBeIssued(IdAustriaScheme.Attributes.PORTRAIT,"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"),
            ClaimToBeIssued(IdAustriaScheme.Attributes.MAIN_ADDRESS, "ewoiR2VtZWluZGVrZW5uemlmZmVyIjoiMDk5ODgiLAoiR2VtZWluZGViZXplaWNobnVuZyI6IlRlc3RnZW1laW5kZSIsCiJQb3N0bGVpdHphaGwiOiIwMDg4IiwKIk9ydHNjaGFmdCI6IlRlc3RvcnQgQSIsCiJTdHJhc3NlIjoiVGVzdGdhc3NlIiwKIkhhdXNudW1tZXIiOiIxYS0yYiIsCiJTdGllZ2UiOiJTdGcuIDNjLTRkIiwKIlR1ZXIiOiJENiIKfQ=="),
            ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_14, true),
            ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_16, true),
            ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_18, true),
            ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_21, true),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun givenNewAppInstallation_whenStartingApp_thenLoadAttributesAndShowData() = runComposeUiTest() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val dummyDataStoreService = DummyDataStoreService()
                val ks = KeystoreService(dummyDataStoreService)
                val walletMain = WalletMain(
                    cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
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
            val responseGenerateRequest = client.post("https://apps.egiz.gv.at/customverifier/transaction/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<JsonObject>()
            val qrCodeUrl = responseGenerateRequest["qrCodeUrl"]?.jsonPrimitive?.content
            val id = responseGenerateRequest["id"]?.jsonPrimitive?.content
            appLink.value = qrCodeUrl

            waitUntilExactlyOneExists(hasText(getString(Res.string.button_label_consent)), 2000)
            onNodeWithText(getString(Res.string.button_label_consent)).performClick()

            val url = "https://apps.egiz.gv.at/customverifier/customer-success.html?id=$id"
            val responseSuccess = client.get(url)
            assertTrue { responseSuccess.status.value in 200..299 }
        }
    }

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

private class TestLifecycleOwner : LifecycleOwner {
    private val _lifecycle = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycle
}

@Serializable
data class RequestBody(val urlprefix: String, val credentials: List<Credential>)

@Serializable
data class Credential(val credentialType: String, val representation: String, val attributes: List<String>)

@Composable
expect fun getPlatformAdapter(): PlatformAdapter

