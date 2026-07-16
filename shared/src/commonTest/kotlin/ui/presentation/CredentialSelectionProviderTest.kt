package ui.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialSelectionProviderTest {
    @Test
    fun detectsPresentationExchangeDescriptorsWithoutMatches() {
        assertTrue(
            hasMissingPresentationExchangeInputDescriptorMatches(
                mapOf(
                    "matched" to mapOf("credential" to Unit),
                    "missing" to emptyMap(),
                )
            )
        )
    }

    @Test
    fun acceptsPresentationExchangeDescriptorsWithMatches() {
        assertFalse(
            hasMissingPresentationExchangeInputDescriptorMatches(
                mapOf(
                    "matched" to mapOf("credential" to Unit),
                )
            )
        )
    }
}
