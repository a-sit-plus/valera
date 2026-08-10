package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import kotlin.test.Test
import kotlin.test.assertNull

class IntentStateTest {
    @Test
    fun resetClearsPendingVerificationIssuance() {
        val intentState = IntentState()
        intentState.pendingDCAPIVerificationIssuance.value = DCAPICredentialType(
            representation = DCAPICredentialRepresentation.SD_JWT,
            type = "urn:example:credential",
        )

        intentState.reset()

        assertNull(intentState.pendingDCAPIVerificationIssuance.value)
    }
}
