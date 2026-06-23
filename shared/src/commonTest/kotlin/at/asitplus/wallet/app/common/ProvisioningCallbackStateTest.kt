package at.asitplus.wallet.app.common

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProvisioningCallbackStateTest {
    @Test
    fun acceptsMatchingCallbackState() {
        validateProvisioningCallbackState(
            redirectedUrl = "${IntentStateTestConstants.PROVISIONING_CALLBACK_URI}?code=abc&state=current-state",
            expectedState = "current-state",
        )
    }

    @Test
    fun rejectsMissingCallbackState() {
        assertFailsWith<IllegalArgumentException> {
            validateProvisioningCallbackState(
                redirectedUrl = "${IntentStateTestConstants.PROVISIONING_CALLBACK_URI}?code=abc",
                expectedState = "current-state",
            )
        }
    }

    @Test
    fun rejectsStaleCallbackState() {
        assertFailsWith<IllegalArgumentException> {
            validateProvisioningCallbackState(
                redirectedUrl = "${IntentStateTestConstants.PROVISIONING_CALLBACK_URI}?code=abc&state=old-state",
                expectedState = "current-state",
            )
        }
    }
}

private object IntentStateTestConstants {
    const val PROVISIONING_CALLBACK_URI = "asitplus-wallet://wallet.a-sit.at/app/callback/provisioning"
}
