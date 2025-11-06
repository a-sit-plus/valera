@file:OptIn(ExperimentalTestApi::class)
package at.asitplus.wallet.app

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.BuildType
import at.asitplus.wallet.app.common.KeystoreService
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.KeyMaterial
import data.storage.DummyDataStoreService
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.multipaz.prompt.PassphraseRequest
import org.multipaz.prompt.PromptModel
import org.multipaz.prompt.SinglePromptModel

@TestRegistering
fun TestSuite.composeTest(
    name: String,
    action: ComposeUiTest.() -> Unit
) = test(name) {
    runComposeUiTest {
        action()
    }
}

val ComposeMultiplatformTests by testSuite {
    composeTest("click") {
        setContent {
            Button({}){
                Text("Button")
            }
        }
        onNodeWithText("Button").performClick()
        onNodeWithText("Button").assertExists()
    }
}
val request = Json.encodeToString(
    RequestBody.serializer(), RequestBody(
        "presentation_definition", listOf(
            Credential(
                EuPidScheme.sdJwtType, "SD_JWT", listOf(
                    EuPidScheme.Attributes.GIVEN_NAME,
                    EuPidScheme.Attributes.FAMILY_NAME,
                    EuPidScheme.Attributes.BIRTH_DATE,
                    EuPidScheme.Attributes.PORTRAIT,
                    EuPidScheme.Attributes.AGE_OVER_18,
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
    ClaimToBeIssued(EuPidScheme.Attributes.GIVEN_NAME, "XXXÉliás"),
    ClaimToBeIssued(EuPidScheme.Attributes.FAMILY_NAME, "XXXTörőcsik"),
    ClaimToBeIssued(EuPidScheme.Attributes.BIRTH_DATE, "1965-10-11"),
    ClaimToBeIssued(
        EuPidScheme.Attributes.PORTRAIT,
        "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAIAAACRXR/mAAAAdklEQVR4nOzQMQ2AQBQEUSAowQcy0IADSnqEoQbKu/40TLLFL2YEbF523Y53CnXeV2pqSQ1lk0WSRZJFkkWSRZJFkkWSRZJFkkWSRSrKmv+npba+vqemir4liySLJIskiySLJIskiySLJIskiySLVJQ1AgAA//81XweDWRWyzwAAAABJRU5ErkJggg=="
    ),
    ClaimToBeIssued(EuPidScheme.Attributes.AGE_OVER_18, true),
)


private fun createWalletDependencyProvider(platformAdapter: PlatformAdapter): WalletDependencyProvider {
    val dummyDataStoreService = DummyDataStoreService()
    val ks = object : KeystoreService(dummyDataStoreService) {
        override suspend fun getSigner(): KeyMaterial = EphemeralKeyWithSelfSignedCert()
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
