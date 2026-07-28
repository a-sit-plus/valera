package at.asitplus.wallet.app

import IosPlatformAdapter
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.dcapi.IosDCAPIInvocationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IosDcApiErrorHandlingTest {
    @Test
    fun validationFailureDoesNotUseCancellationCallback() {
        val intentState = IntentState()
        var validationError: String? = null
        var cancelled = false
        intentState.dcapiInvocationData.value = IosDCAPIInvocationData(
            rawRequest = null,
            parsedRequestSummary = null,
            origin = null,
            sendCredentialResponse = {},
            sendCredentialError = { validationError = it },
            onCancel = { cancelled = true },
        )

        IosPlatformAdapter(intentState).prepareDCAPICredentialError("OAuth error must stay diagnostic-only")

        assertEquals("ISO 18013-7 Annex C request validation failed", validationError)
        assertFalse(cancelled)
    }
}
