package ui.viewmodels.intents

import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DCAPIAuthorizationIntentViewModelTest {
    private val mdl = type("org.iso.18013.5.1.mDL")
    private val pid = type("eu.europa.ec.eudi.pid.1")

    @Test
    fun findsDistinctSupportedMissingTypesInRequestOrder() {
        assertEquals(
            listOf(mdl, pid),
            missingIssuableCredentialTypes(
                requestedDocTypes = listOf(mdl.type, "unsupported", mdl.type, pid.type),
                hasMatches = listOf(false, false, false, false),
                supportedTypes = setOf(pid, mdl),
            ),
        )
    }

    @Test
    fun ignoresRequestsWithCredentialMatches() {
        assertEquals(
            listOf(pid),
            missingIssuableCredentialTypes(
                requestedDocTypes = listOf(mdl.type, pid.type),
                hasMatches = listOf(true, false),
                supportedTypes = setOf(mdl, pid),
            ),
        )
    }

    @Test
    fun rejectsMismatchedRequestAndMatchCounts() {
        assertFailsWith<IllegalArgumentException> {
            missingIssuableCredentialTypes(
                requestedDocTypes = listOf(mdl.type),
                hasMatches = emptyList(),
                supportedTypes = setOf(mdl),
            )
        }
    }

    private fun type(docType: String) =
        DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, docType)
}
