package at.asitplus.wallet.app

import IOS_SUPPORTED_DOC_TYPES
import IosPlatformAdapter
import iosIssuingDocumentId
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.app.common.IntentState
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import at.asitplus.wallet.app.common.dcapi.DCAPIVerificationData
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IosDcApiVerificationIssuanceTest {
    @Test
    fun supportedTypesMirrorEntitlements() {
        assertEquals(
            setOf(
                "org.iso.23220.photoid.1",
                EU_PID_DOCTYPE,
                MDL_DOCTYPE,
                AV_DOC_TYPE,
            ),
            IOS_SUPPORTED_DOC_TYPES,
        )
    }

    @Test
    fun issuingDocumentIdsAreStableAndTypeSpecific() {
        val ageVerificationId = iosIssuingDocumentId(AV_DOC_TYPE)

        assertEquals(ageVerificationId, iosIssuingDocumentId(AV_DOC_TYPE))
        assertNotEquals(ageVerificationId, iosIssuingDocumentId(MDL_DOCTYPE))
        assertTrue(ageVerificationId.startsWith("dcapi-issuing-credential:mdoc:"))
    }

    @Test
    fun resolvesIssuanceQueueInOrder() {
        val intentState = IntentState()
        val adapter = IosPlatformAdapter(intentState)
        val mdl = DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, MDL_DOCTYPE)
        val pid = DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, EU_PID_DOCTYPE)
        intentState.pendingDCAPIVerificationIssuanceQueue.value = listOf(mdl, pid)

        assertEquals(mdl, assertIs<DCAPIVerificationData.IssuanceRequired>(
            adapter.getCurrentDCAPIVerificationData().getOrThrow()
        ).credentialType)
        adapter.resolveCurrentDCAPIVerificationIssuance(mdl, "mdl-id").getOrThrow()
        assertEquals(pid, assertIs<DCAPIVerificationData.IssuanceRequired>(
            adapter.getCurrentDCAPIVerificationData().getOrThrow()
        ).credentialType)

        assertFailsWith<IllegalArgumentException> {
            adapter.resolveCurrentDCAPIVerificationIssuance(mdl, "wrong-order").getOrThrow()
        }
    }
}
