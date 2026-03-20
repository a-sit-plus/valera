package ui.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CredentialSelectionProviderTest {
    @Test
    fun rejectsPresentationExchangeDescriptorsWithoutMatches() {
        val error = assertFailsWith<IllegalStateException> {
            validatePresentationExchangeInputDescriptorMatches(
                mapOf(
                    "matched" to mapOf("credential" to Unit),
                    "missing" to emptyMap(),
                )
            )
        }

        assertEquals(
            "Presentation definition input descriptor(s) missing did not match any stored credential",
            error.message,
        )
    }

    @Test
    fun acceptsPresentationExchangeDescriptorsWithMatches() {
        validatePresentationExchangeInputDescriptorMatches(
            mapOf(
                "matched" to mapOf("credential" to Unit),
            )
        )
    }
}
