package ui.presentation

import at.asitplus.wallet.lib.agent.HolderIsoDeviceRetrievalQueryMatchingResult
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalClaimMatch
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalCredentialMatch
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalQueryMatchingResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialSelectionProviderTest {

    @Test
    fun detectsDocumentRequestsWithoutMatches() {
        assertTrue(matchingResult(listOf(listOf(match()), emptyList())).hasUnsatisfiedDocumentRequest())
    }

    @Test
    fun acceptsDocumentRequestsWithMatches() {
        assertFalse(matchingResult(listOf(listOf(match()), listOf(match()))).hasUnsatisfiedDocumentRequest())
    }

    /**
     * A Device Request without any document request has nothing to select, so it must route to the no-credential
     * screen rather than to a selection step that would index an empty list.
     */
    @Test
    fun detectsDeviceRequestWithoutDocumentRequests() {
        assertTrue(matchingResult(emptyList()).hasUnsatisfiedDocumentRequest())
    }

    private fun matchingResult(matches: List<List<IsoDeviceRetrievalCredentialMatch>>) =
        HolderIsoDeviceRetrievalQueryMatchingResult(
            credentials = listOf("credential"),
            queryMatchingResult = IsoDeviceRetrievalQueryMatchingResult(matches),
        )

    private fun match() = IsoDeviceRetrievalCredentialMatch(
        credentialIndex = 0,
        requestedClaims = listOf(IsoDeviceRetrievalClaimMatch("ns", "given_name", "value")),
    )
}
