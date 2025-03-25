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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_accept
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.button_label_details
import at.asitplus.valera.resources.button_label_start
import at.asitplus.valera.resources.content_description_portrait
import at.asitplus.valera.resources.section_heading_age_data
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletCryptoService
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import data.storage.DummyDataStoreService
import io.kotest.common.Platform
import io.kotest.common.platform
import io.kotest.core.spec.style.FunSpec
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import ui.navigation.NavigatorTestTags
import ui.navigation.routes.OnboardingWrapperTestTags
import ui.views.OnboardingStartScreenTestTag
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private lateinit var lifecycleRegistry: LifecycleRegistry
private lateinit var lifecycleOwner: TestLifecycleOwner

@OptIn(ExperimentalTestApi::class)
class InstrumentedTestsSuite : FunSpec({

    beforeTest {
        lifecycleOwner = TestLifecycleOwner()
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
        //android needs main, iOS probably too, but it hangs on iOS, so we let it at least fail
        withContext(if (platform != Platform.Native) Dispatchers.Main else Dispatchers.Unconfined) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    afterTest {
        //android needs main, iOS probably too, but it hangs on iOS, so we let it at least fail
        withContext(if (platform != Platform.Native) Dispatchers.Main else Dispatchers.Unconfined) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    context("Starting App Tests") {
        test("App should start correctly") {
            runComposeUiTest {
                setContent {
                    val platformAdapter = getPlatformAdapter()
                    val walletMain = createWalletMain(platformAdapter)
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
        }

        test("Test 2: App should display onboarding screen") {
            runComposeUiTest {
                setContent {
                    val platformAdapter = getPlatformAdapter()
                    val walletMain = createWalletMain(platformAdapter)
                    App(walletMain)
                }

                waitUntil {
                    onNodeWithTag(NavigatorTestTags.loadingTestTag)
                        .isNotDisplayed()
                }

                onNodeWithTag(OnboardingWrapperTestTags.onboardingStartScreen)
                    .assertIsDisplayed()

            }
        }

        test("Test 3: App should show onboarding start button") {
            runComposeUiTest {
                setContent {
                    val platformAdapter = getPlatformAdapter()
                    val walletMain = createWalletMain(platformAdapter)
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
        }
    }

    context("End to End Tests") {
        test("End to End Test 1: Should complete the process") {
            runComposeUiTest {
                lateinit var walletMain: WalletMain
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                        val platformAdapter = getPlatformAdapter()
                        walletMain = createWalletMain(platformAdapter)
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
                    waitUntilExactlyOneExists(hasText(getString(Res.string.button_label_start)))
                    onNodeWithText(getString(Res.string.button_label_start)).performClick()
                    onNodeWithText(getString(Res.string.button_label_continue))
                        .assertIsDisplayed()
                    onNodeWithText(getString(Res.string.button_label_continue)).performClick()
                    onNodeWithText(getString(Res.string.button_label_accept))
                        .assertIsDisplayed()
                    onNodeWithText(getString(Res.string.button_label_accept)).performClick()
                    waitUntilDoesNotExist(
                        hasText(getString(Res.string.button_label_accept)),
                        10000
                    )

                    onNodeWithContentDescription(getString(Res.string.content_description_portrait)).assertHeightIsAtLeast(
                        1.dp
                    )
                    onNodeWithText("XXXÉliás XXXTörőcsik").assertExists()
                    onNodeWithText("11.10.1965").assertExists()

                    onNodeWithText(getString(Res.string.button_label_details)).performClick()
                    waitUntilExactlyOneExists(
                        hasText(getString(Res.string.section_heading_age_data)),
                        3000
                    )
                    onNodeWithText("≥14").assertExists()
                    onNodeWithText("≥16").assertExists()
                    onNodeWithText("≥18").assertExists()
                    onNodeWithText("≥21").assertExists()
                    onNodeWithText("Testgasse 1a-2b/Stg. 3c-4d/D6").assertExists()
                    onNodeWithText("0088 Testort A").assertExists()

                    onNodeWithText(getString(Res.string.button_label_details)).performClick()


                    val client = HttpClient {
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

                    val firstProfile =
                        responseGenerateRequest["profiles"]?.jsonArray?.first()?.jsonObject
                    val qrCodeUrl = firstProfile?.get("url")?.jsonPrimitive?.content
                    val id = firstProfile?.get("id")?.jsonPrimitive?.content

                    Globals.appLink.value = qrCodeUrl!!

                    waitUntilExactlyOneExists(
                        hasText(getString(Res.string.button_label_continue)),
                        10000
                    )

                    onNodeWithText(getString(Res.string.button_label_continue)).performClick()

                    val url = "https://apps.egiz.gv.at/customverifier/customer-success.html?id=$id"
                    val responseSuccess = client.get(url)
                    assertTrue { responseSuccess.status.value in 200..299 }
                }
            }
        }
    }
})

val request = Json.encodeToString(
    RequestBody.serializer(),
    RequestBody(
        "presentation_definition",
        listOf(
            Credential(
                "at.gv.id-austria.2023.1",
                "SD_JWT",
                listOf(
                    IdAustriaScheme.Attributes.BPK,
                    IdAustriaScheme.Attributes.FIRSTNAME,
                    IdAustriaScheme.Attributes.LASTNAME,
                    IdAustriaScheme.Attributes.DATE_OF_BIRTH,
                    IdAustriaScheme.Attributes.PORTRAIT,
                    IdAustriaScheme.Attributes.MAIN_ADDRESS,
                    IdAustriaScheme.Attributes.AGE_OVER_18,
                )
            )
        )
    )
)

@Serializable
data class RequestBody(
    val presentationMechanismIdentifier: String,
    val credentials: List<Credential>
)

@Serializable
data class Credential(
    val credentialType: String,
    val representation: String,
    val attributes: List<String>
)

@Composable
expect fun getPlatformAdapter(): PlatformAdapter


private fun getAttributes(): List<ClaimToBeIssued> = listOf(
    ClaimToBeIssued(IdAustriaScheme.Attributes.BPK, "XFN+436920f:L9LBxmjNPt0041j5O1+sir0HOG0="),
    ClaimToBeIssued(IdAustriaScheme.Attributes.FIRSTNAME, "XXXÉliás"),
    ClaimToBeIssued(IdAustriaScheme.Attributes.LASTNAME, "XXXTörőcsik"),
    ClaimToBeIssued(IdAustriaScheme.Attributes.DATE_OF_BIRTH, "1965-10-11"),
    ClaimToBeIssued(
        IdAustriaScheme.Attributes.PORTRAIT,
        "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAIAAACRXR/mAAAAdklEQVR4nOzQMQ2AQBQEUSAowQcy0IADSnqEoQbKu/40TLLFL2YEbF523Y53CnXeV2pqSQ1lk0WSRZJFkkWSRZJFkkWSRZJFkkWSRSrKmv+npba+vqemir4liySLJIskiySLJIskiySLJIskiySLVJQ1AgAA//81XweDWRWyzwAAAABJRU5ErkJggg=="
    ),
    ClaimToBeIssued(
        IdAustriaScheme.Attributes.MAIN_ADDRESS,
        "ewoiR2VtZWluZGVrZW5uemlmZmVyIjoiMDk5ODgiLAoiR2VtZWluZGViZXplaWNobnVuZyI6IlRlc3RnZW1laW5kZSIsCiJQb3N0bGVpdHphaGwiOiIwMDg4IiwKIk9ydHNjaGFmdCI6IlRlc3RvcnQgQSIsCiJTdHJhc3NlIjoiVGVzdGdhc3NlIiwKIkhhdXNudW1tZXIiOiIxYS0yYiIsCiJTdGllZ2UiOiJTdGcuIDNjLTRkIiwKIlR1ZXIiOiJENiIKfQ=="
    ),
    ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_14, true),
    ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_16, true),
    ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_18, true),
    ClaimToBeIssued(IdAustriaScheme.Attributes.AGE_OVER_21, true),
)


private fun createWalletMain(platformAdapter: PlatformAdapter): WalletMain {
    val dummyDataStoreService = DummyDataStoreService()
    val ks = object : KeystoreService(dummyDataStoreService) {
        override suspend fun getSigner(): KeyMaterial = EphemeralKeyWithSelfSignedCert()
    }
    return WalletMain(
        cryptoService = ks.let { runBlocking { WalletCryptoService(it.getSigner()) } },
        dataStoreService = dummyDataStoreService,
        platformAdapter = platformAdapter,
        buildContext = BuildContext(
            buildType = BuildType.DEBUG,
            packageName = "test",
            versionCode = 0,
            versionName = "0.0.0",
            osVersion = "Unit Test"
        ),
        scope = CoroutineScope(Dispatchers.Default),
    )
}

class TestLifecycleOwner : LifecycleOwner {
    private val _lifecycle = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycle
}