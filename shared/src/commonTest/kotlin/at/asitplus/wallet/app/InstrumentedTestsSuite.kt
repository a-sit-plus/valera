package at.asitplus.wallet.app

import App
import Globals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.button_label_start
import at.asitplus.valera.resources.content_description_portrait
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.CapabilitiesData
import at.asitplus.wallet.app.common.CapabilitiesService
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.rfc3986.toUri
import data.storage.DummyDataStoreService
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.prompt.PassphraseRequest
import org.multipaz.prompt.PromptModel
import org.multipaz.prompt.SinglePromptModel
import ui.navigation.routes.RoutePrerequisites
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.endToEndTest() {
    val startText = runBlocking { getString(Res.string.button_label_start) }
    val portraitText = runBlocking { getString(Res.string.content_description_portrait) }
    val continueText = runBlocking { getString(Res.string.button_label_continue) }

    val client = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }

    setContent {
        // A. Create the dependency provider, remembering it against the platformAdapter
        //    so it's not recreated unnecessarily.
        val platformAdapter = getPlatformAdapter()

        val walletDependencyProvider = remember(platformAdapter) {
            createWalletDependencyProvider(platformAdapter)
        }
        
        val capabilitiesModule = module {
            scope(named(SESSION_NAME)) {
                scopedOf(::DummyCapabilitiesService) binds arrayOf(CapabilitiesService::class)
            }
        }
        val module = appModule(walletDependencyProvider, capabilitiesModule)

        // B. Call the main App composable within the CompositionLocalProvider.
        CompositionLocalProvider(
            LocalLifecycleOwner provides TestLifecycleOwner()
        ) {
            App(module)
        }

        // C. Inject services after framework is running
        val sessionService: SessionService = koinInject()
        val holderAgent: HolderAgent = koinInject(scope = sessionService.scope.value)

        // D. Use LaunchedEffect for one-time, asynchronous setup tasks.
        //    This is the correct way to run non-UI suspend functions from a Composable.
        LaunchedEffect(Unit) {
            val issuer = IssuerAgent(
                keyMaterial = EphemeralKeyWithoutCert(),
                statusListBaseUrl = "https://wallet.a-sit.at/m6/credentials/status",
                identifier = "https://issuer.example.com/".toUri(),
            )
            holderAgent.storeCredential(
                issuer.issueCredential(
                    CredentialToBeIssued.VcSd(
                        getAttributes(),
                        Clock.System.now().plus(60.minutes),
                        EuPidSdJwtScheme,
                        holderAgent.keyMaterial.publicKey,
                        OidcUserInfoExtended(userInfo = OidcUserInfo(subject = ""))
                    )
                ).getOrThrow().toStoreCredentialInput()
            )
        }
    }
    waitUntilExactlyOneExists(hasText(startText))
    onNodeWithText(startText).performClick()
    onNodeWithText(continueText).performClick()
    waitUntilDoesNotExist(hasText(continueText), 10000)

    onNodeWithContentDescription(portraitText).assertHeightIsAtLeast(1.dp)
    onNodeWithText("XXXÉliás XXXTörőcsik").assertExists()
    onNodeWithText("11.10.1965").assertExists()

    val responseGenerateRequest = runBlocking {
        client.post("https://apps.egiz.gv.at/customverifier/transaction/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<JsonObject>()
    }

    val firstProfile = responseGenerateRequest["profiles"]?.jsonArray?.first()?.jsonObject
    val qrCodeUrl = firstProfile?.get("url")?.jsonPrimitive?.content
    val id = firstProfile?.get("id")?.jsonPrimitive?.content

    Globals.appLink.value = qrCodeUrl!!

    waitUntilExactlyOneExists(hasText(continueText), 10000)

    onNodeWithText(continueText).performClick()

    val url = "https://apps.egiz.gv.at/customverifier/customer-success.html?id=$id"
    val responseSuccess = runBlocking { client.get(url) }
    assertTrue { responseSuccess.status.value in 200..299 }
}

val request = Json.encodeToString(
    RequestBody.serializer(), RequestBody(
        "presentation_definition", listOf(
            Credential(
                credentialType = EuPidSdJwtScheme.sdJwtType,
                representation = "SD_JWT",
                attributes = listOf(
                    EuPidSdJwtScheme.SdJwtAttributes.GIVEN_NAME,
                    EuPidSdJwtScheme.SdJwtAttributes.FAMILY_NAME,
                    EuPidSdJwtScheme.SdJwtAttributes.BIRTH_DATE,
                    EuPidSdJwtScheme.SdJwtAttributes.PORTRAIT,
                )
            )
        )
    )
)

@Serializable
data class RequestBody(
    val presentationMechanismIdentifier: String, val credentials: List<Credential>
)

@Serializable
data class Credential(
    val credentialType: String, val representation: String, val attributes: List<String>
)

@Composable
expect fun getPlatformAdapter(): PlatformAdapter


private fun getAttributes(): List<ClaimToBeIssued> = listOf(
    ClaimToBeIssued(EuPidSdJwtScheme.SdJwtAttributes.GIVEN_NAME, "XXXÉliás"),
    ClaimToBeIssued(EuPidSdJwtScheme.SdJwtAttributes.FAMILY_NAME, "XXXTörőcsik"),
    ClaimToBeIssued(EuPidSdJwtScheme.SdJwtAttributes.BIRTH_DATE, "1965-10-11"),
    ClaimToBeIssued(
        EuPidSdJwtScheme.SdJwtAttributes.PORTRAIT,
        "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAIAAACRXR/mAAAAdklEQVR4nOzQMQ2AQBQEUSAowQcy0IADSnqEoQbKu/40TLLFL2YEbF523Y53CnXeV2pqSQ1lk0WSRZJFkkWSRZJFkkWSRZJFkkWSRSrKmv+npba+vqemir4liySLJIskiySLJIskiySLJIskiySLVJQ1AgAA//81XweDWRWyzwAAAABJRU5ErkJggg=="
    ),
)


private fun createWalletDependencyProvider(platformAdapter: PlatformAdapter): WalletDependencyProvider {
    val dummyDataStoreService = DummyDataStoreService()
    val ks = object : KeystoreService(dummyDataStoreService) {
        override suspend fun getSigner(): KeyMaterial = EphemeralKeyWithSelfSignedCert()
        override suspend fun testSigner() = catchingUnwrapped { getSigner() }.isSuccess
    }
    return WalletDependencyProvider(
        keystoreService = ks,
        dataStoreService = dummyDataStoreService,
        platformAdapter = platformAdapter,
        buildContext = BuildContext(
            buildType = BuildType.DEBUG,
            packageName = "test",
            versionCode = 0,
            versionName = "0.0.0",
            osVersion = "Unit Test"
        ),
        promptModel = TestPromptModel(),
    )
}

class TestLifecycleOwner : LifecycleOwner {
    private val _lifecycle = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycle
}

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp
class TestPromptModel : PromptModel {
    override val passphrasePromptModel = SinglePromptModel<PassphraseRequest, String?>()
    override val promptModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + this)

    fun onClose() {
        promptModelScope.cancel()
    }
}

class DummyCapabilitiesService : CapabilitiesService {
    override fun getDeviceStatus(): Flow<CapabilitiesData?> =
        flow { emit(CapabilitiesData(true, true, true, true, true, true)) }

    override suspend fun refreshStatus() {
    }

    override suspend fun reset() {
    }

    override fun evaluatePrerequisites(list: Set<RoutePrerequisites>): Flow<Boolean> = flow { emit(true) }

}