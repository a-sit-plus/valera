package at.asitplus.wallet.app.android.dcapi

import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DCAPIVerificationSelectionTest {
    private val mdocType = DCAPICredentialType(
        DCAPICredentialRepresentation.ISO_MDOC,
        "org.iso.18013.5.1.mDL",
    )
    private val sdJwtType = DCAPICredentialType(
        DCAPICredentialRepresentation.SD_JWT,
        "urn:example:pid",
    )

    @Test
    fun resolvesSyntheticSelectionsInOrderAndReplacesDuplicates() {
        val selection = DCAPIVerificationSelection(
            protocol = "openid4vp-v1-unsigned",
            documentIds = listOf(
                "stored-id",
                syntheticId("mdoc", mdocType.type),
                syntheticId("mdoc", mdocType.type),
                syntheticId("sdjwt", sdJwtType.type),
            ),
        )

        assertEquals(mdocType, selection.nextIssuanceType())
        selection.resolve(mdocType, "new-mdoc-id")
        assertEquals(sdJwtType, selection.nextIssuanceType())
        selection.resolve(sdJwtType, "new-sdjwt-id")
        assertEquals(
            listOf("stored-id", "new-mdoc-id", "new-mdoc-id", "new-sdjwt-id"),
            selection.resolvedDocumentIds(),
        )
    }

    @Test
    fun rejectsMalformedAndOutOfOrderSyntheticSelections() {
        assertFailsWith<IllegalArgumentException> {
            SyntheticCredentialId.decode("dcapi-issuing-credential:mdoc:not-hex")
        }
        val selection = DCAPIVerificationSelection(
            "protocol",
            listOf(syntheticId("mdoc", mdocType.type)),
        )
        assertFailsWith<IllegalArgumentException> {
            selection.resolve(sdJwtType, "wrong-id")
        }
    }

    private fun syntheticId(representation: String, type: String): String =
        "dcapi-issuing-credential:$representation:${type.encodeToByteArray().toHex()}"

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }
}
