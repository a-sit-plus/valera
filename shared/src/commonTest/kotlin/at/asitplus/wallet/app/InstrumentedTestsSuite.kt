package at.asitplus.wallet.app

import App
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasContentDescription
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
import at.asitplus.signum.indispensable.io.Base64Strict
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
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.SessionHandle
import at.asitplus.wallet.app.common.SessionService
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletSessionBindings
import at.asitplus.wallet.app.common.di.appModule
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements
import at.asitplus.wallet.lib.RequestOptionsCredential
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.SdJwtCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.openid.ClientIdScheme
import at.asitplus.wallet.lib.openid.CredentialPresentationRequestBuilder
import at.asitplus.wallet.lib.openid.OpenId4VpRequestOptions
import at.asitplus.wallet.lib.openid.OpenId4VpVerifier
import data.storage.AntilogAdapter
import data.storage.DummyDataStoreService
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.multipaz.prompt.PromptModel
import org.multipaz.prompt.Reason
import ui.navigation.routes.RoutePrerequisites
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTestApi::class)
@ExperimentalMaterial3Api
fun ComposeUiTest.endToEndTest() {
    val startText = runBlocking { getString(Res.string.button_label_start) }
    val portraitText = runBlocking { getString(Res.string.content_description_portrait) }
    val continueText = runBlocking { getString(Res.string.button_label_continue) }
    val pidHeader = runBlocking { getString(Res.string.credential_scheme_label_eu_pid_sdjwt) }
    val openUrlText = runBlocking { getString(Res.string.button_label_open_url) }
    val authenticationSuccessText = runBlocking { getString(Res.string.heading_label_authentication_success) }
    val redirectUrl = CompletableDeferred<String>()
    val credentialIssued = CompletableDeferred<Unit>()
    val intentState = IntentState()

    setContent {
        val platformAdapter = getPlatformAdapter()

        val walletDependencyProvider = remember(platformAdapter) {
            createWalletDependencyProvider(
                RecordingPlatformAdapter(platformAdapter) {
                    redirectUrl.complete(it)
                }
            )
        }

        val capabilitiesModule = remember {
            module {
                scope(named(SESSION_NAME)) {
                    scopedOf(::DummyCapabilitiesService) binds arrayOf(CapabilitiesService::class)
                }
            }
        }

        KoinApplication(
            configuration = koinConfiguration {
                modules(appModule(), capabilitiesModule, module { single { walletDependencyProvider.buildContext } })
            }
        ) {
            val sessionService = remember(walletDependencyProvider, intentState) {
                SessionService().apply {
                    initialize {
                        val sessionCoroutineScope = CoroutineScope(
                            SupervisorJob() + Dispatchers.Default
                        )
                        val scope = KoinPlatform.getKoin().createScope(
                            "test-session:${Uuid.random()}",
                            named(SESSION_NAME)
                        )
                        scope.declare(
                            WalletSessionBindings(
                                intentState = intentState,
                                sessionService = this,
                                buildContext = walletDependencyProvider.buildContext,
                                promptModel = walletDependencyProvider.promptModel,
                                platformAdapter = walletDependencyProvider.platformAdapter,
                                dataStoreService = walletDependencyProvider.dataStoreService,
                                keystoreService = walletDependencyProvider.keystoreService,
                                sessionCoroutineScope = sessionCoroutineScope
                            )
                        )
                        SessionHandle(scope = scope) {
                            sessionCoroutineScope.cancel()
                        }
                    }
                }
            }

            CompositionLocalProvider(
                LocalLifecycleOwner provides TestLifecycleOwner()
            ) {
                App(
                    sessionService = sessionService,
                    intentState = intentState
                )
            }

            val holderAgent: HolderAgent = koinInject(scope = sessionService.scope.value)

            LaunchedEffect(Unit) {
                println("InstrumentedTests: starting credential issuance setup")
                val issuer = IssuerAgent(
                    keyMaterial = EphemeralKeyWithoutCert(),
                    statusListBaseUrl = "http://127.0.0.1/credentials/status",
                    identifier = "https://issuer.example.com/".toUri(),
                )
                catchingUnwrapped {
                    holderAgent.storeCredential(
                        issuer.issueCredential(
                            CredentialToBeIssued.VcSd(
                                getAttributes(),
                                Clock.System.now().plus(60.minutes),
                                pidSdJwtScheme(),
                                holderAgent.keyMaterial.publicKey,
                                OidcUserInfoExtended(userInfo = OidcUserInfo(subject = ""))
                            )
                        )
                            .getOrThrow()
                            .toStoreCredentialInput()
                    )
                }.onSuccess {
                    println("InstrumentedTests: credential issuance setup completed")
                    credentialIssued.complete(Unit)
                }.onFailure {
                    println("InstrumentedTests: credential issuance setup failed: ${it::class.simpleName}: ${it.message}")
                    credentialIssued.completeExceptionally(it)
                    throw it
                }
            }
        }
    }

    waitUntil(timeoutMillis = 10000) { credentialIssued.isCompleted }
    runBlocking { credentialIssued.await() }

    waitUntilExactlyOneExists(hasText(startText))
    onNodeWithText(startText).performClick()
    onNodeWithText(continueText).performClick()
    waitUntilDoesNotExist(hasText(continueText), 10000)

    waitUntilExactlyOneExists(hasContentDescription(portraitText), 10000)
    onNodeWithContentDescription(portraitText).assertHeightIsAtLeast(1.dp)
    onNodeWithText("XXXÉliás XXXTörőcsik").assertExists()
    onNodeWithText("11.10.1965").assertExists()

    val localPresentationRequest = runBlocking { createLocalPresentationRequest() }
    runOnIdle {
        intentState.appLink.value = localPresentationRequest.url
    }

    waitUntilExactlyOneExists(hasText(continueText), 10000)
    onNodeWithText(continueText).performClick()

    waitUntilExactlyOneExists(hasText(pidHeader), 5000)
    onNodeWithText(pidHeader).performClick()

    waitUntilExactlyOneExists(hasText(continueText), 5000)
    onNodeWithText(continueText).performClick()

    waitUntilExactlyOneExists(hasText(authenticationSuccessText), 10000)
    onNodeWithText(openUrlText).performClick()

    val validationResult = runBlocking {
        localPresentationRequest.verifier.validateAuthnResponse(
            withTimeout(10000.milliseconds) { redirectUrl.await() }
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
                    credentialScheme = pidSdJwtScheme(),
                    representation = SD_JWT,
                    requestedAttributes = setOf(
                        EuPidSdJwtDataElements.GIVEN_NAME,
                        EuPidSdJwtDataElements.FAMILY_NAME,
                        EuPidSdJwtDataElements.BIRTH_DATE,
                        EuPidSdJwtDataElements.PORTRAIT,
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
    ClaimToBeIssued(EuPidSdJwtDataElements.GIVEN_NAME, "XXXÉliás"),
    ClaimToBeIssued(EuPidSdJwtDataElements.FAMILY_NAME, "XXXTörőcsik"),
    ClaimToBeIssued(EuPidSdJwtDataElements.BIRTH_DATE, "1965-10-11"),
    ClaimToBeIssued(
        EuPidSdJwtDataElements.PORTRAIT,
        TEST_PORTRAIT_PNG.decodeToByteArray(Base64Strict)
    ),
)

/**
 * A valid 16x16 RGB PNG: the chunk CRCs must be correct, since skia on Android verifies them when
 * decoding, and the image must be large enough to render at a height of at least 1.dp on the device
 */
private const val TEST_PORTRAIT_PNG =
    "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAF0lEQVR4nGM4w8BAEiJN9aiGUQ1DSgMAQWfMAdovJBMAAAAASUVORK5CYII="

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
            osVersion = "Unit Test",
        ),
        promptModel = TestPromptModel(),
        antilog = AntilogAdapter(platformAdapter, "", BuildType.DEBUG),
    )
}
// Scheme is resolved from remote type metadata registered at boot, not the removed library scheme object.
private suspend fun pidSdJwtScheme() =
    AttributeIndex.resolveIdentifier(EU_PID_SD_JWT_VCT, SD_JWT) as? SdJwtCredentialScheme
        ?: SdJwtFallbackCredentialScheme(EU_PID_SD_JWT_VCT)

class TestLifecycleOwner : LifecycleOwner {
    private val _lifecycle = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycle
}

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp
class TestPromptModel private constructor(
    builder: Builder,
) : PromptModel(builder) {
    constructor() : this(Builder())

    override val promptModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + this)

    private class Builder : PromptModel.Builder(
        toHumanReadable = { _, _ ->
            Reason.HumanReadable(
                title = "",
                subtitle = "",
                requireConfirmation = false
            )
        }
    ) {
        init {
            addCommonDialogs()
        }

        override fun build(): PromptModel = TestPromptModel(this)
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
