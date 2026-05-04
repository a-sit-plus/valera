package at.asitplus.wallet.app

import App
import Globals
import androidx.compose.material3.ExperimentalMaterial3Api
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
import at.asitplus.valera.resources.button_label_open_url
import at.asitplus.valera.resources.button_label_start
import at.asitplus.valera.resources.content_description_portrait
import at.asitplus.valera.resources.credential_scheme_label_eu_pid_sdjwt
import at.asitplus.valera.resources.heading_label_authentication_success
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
import at.asitplus.wallet.lib.RequestOptionsCredential
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.openid.ClientIdScheme
import at.asitplus.wallet.lib.openid.CredentialPresentationRequestBuilder
import at.asitplus.wallet.lib.openid.OpenId4VpRequestOptions
import at.asitplus.wallet.lib.openid.OpenId4VpVerifier
import data.storage.DummyDataStoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.prompt.PassphraseRequest
import org.multipaz.prompt.PromptModel
import org.multipaz.prompt.SinglePromptModel
import ui.navigation.routes.RoutePrerequisites
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@ExperimentalMaterial3Api
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.endToEndTest() {
    val startText = runBlocking { getString(Res.string.button_label_start) }
    val portraitText = runBlocking { getString(Res.string.content_description_portrait) }
    val continueText = runBlocking { getString(Res.string.button_label_continue) }
    val pidHeader = runBlocking { getString(Res.string.credential_scheme_label_eu_pid_sdjwt) }
    val openUrlText = runBlocking { getString(Res.string.button_label_open_url) }
    val authenticationSuccessText = runBlocking { getString(Res.string.heading_label_authentication_success) }
    val redirectUrl = CompletableDeferred<String>()

    setContent {
        // A. Create the dependency provider, remembering it against the platformAdapter
        //    so it's not recreated unnecessarily.
        val platformAdapter = getPlatformAdapter()

        val walletDependencyProvider = remember(platformAdapter) {
            createWalletDependencyProvider(
                RecordingPlatformAdapter(platformAdapter) {
                    redirectUrl.complete(it)
                }
            )
        }
        
        val capabilitiesModule = module {
            scope(named(SESSION_NAME)) {
                scopedOf(::DummyCapabilitiesService) binds arrayOf(CapabilitiesService::class)
            }
        }
        val testModule = module {
            includes(appModule(walletDependencyProvider, capabilitiesModule))
            singleOf(::Validator)
        }

        // B. Call the main App composable within the CompositionLocalProvider.
        CompositionLocalProvider(
            LocalLifecycleOwner provides TestLifecycleOwner()
        ) {
            App(testModule)
        }

        // C. Inject services after framework is running
        val sessionService: SessionService = koinInject()
        val holderAgent: HolderAgent = koinInject(scope = sessionService.scope.value)

        // D. Use LaunchedEffect for one-time, asynchronous setup tasks.
        //    This is the correct way to run non-UI suspend functions from a Composable.
        LaunchedEffect(Unit) {
            val issuer = IssuerAgent(
                keyMaterial = EphemeralKeyWithoutCert(),
                statusListBaseUrl = "http://127.0.0.1/credentials/status",
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

    val localPresentationRequest = runBlocking { createLocalPresentationRequest() }
    Globals.appLink.value = localPresentationRequest.url

    // Confirm first consent screen
    waitUntilExactlyOneExists(hasText(continueText), 10000)
    onNodeWithText(continueText).performClick()

    // Select credential from list
    waitUntilExactlyOneExists(hasText(pidHeader), 5000)
    onNodeWithText(pidHeader).performClick()

    // Confirm second screen
    waitUntilExactlyOneExists(hasText(continueText), 500)
    onNodeWithText(continueText).performClick()

    // Wait for header after posting the result
    waitUntilExactlyOneExists(hasText(authenticationSuccessText), 10000)
    onNodeWithText(openUrlText).performClick()

    val validationResult = runBlocking {
        localPresentationRequest.verifier.validateAuthnResponse(
            withTimeout(10000) { redirectUrl.await() }
        ).getOrThrow()
    }
    assertNotNull(validationResult.vpTokenValidationResult?.getOrThrow())
}

@Composable
expect fun getPlatformAdapter(): PlatformAdapter

private data class LocalPresentationRequest(
    val url: String,
    val verifier: OpenId4VpVerifier,
)

private suspend fun createLocalPresentationRequest(): LocalPresentationRequest {
    val verifier = OpenId4VpVerifier(
        keyMaterial = EphemeralKeyWithoutCert(),
        clientIdScheme = ClientIdScheme.RedirectUri("https://wallet.example.org/return"),
    )
    val requestOptions = OpenId4VpRequestOptions(
        presentationRequest = CredentialPresentationRequestBuilder(
            credentials = setOf(
                RequestOptionsCredential(
                    credentialScheme = EuPidSdJwtScheme,
                    representation = SD_JWT,
                    requestedAttributes = setOf(
                        EuPidSdJwtScheme.SdJwtAttributes.GIVEN_NAME,
                        EuPidSdJwtScheme.SdJwtAttributes.FAMILY_NAME,
                        EuPidSdJwtScheme.SdJwtAttributes.BIRTH_DATE,
                        EuPidSdJwtScheme.SdJwtAttributes.PORTRAIT,
                    ),
                )
            )
        ).toPresentationExchangeRequest()
    )

    return LocalPresentationRequest(
        url = verifier.createAuthnRequest(
            requestOptions = requestOptions,
            creationOptions = OpenId4VpVerifier.CreationOptions.Query("https://wallet.example.org/authorize"),
        ).getOrThrow().url,
        verifier = verifier,
    )
}

private class RecordingPlatformAdapter(
    private val delegate: PlatformAdapter,
    private val onOpenUrl: (String) -> Unit,
) : PlatformAdapter by delegate {
    override fun openUrl(url: String) {
        onOpenUrl(url)
    }
}

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
