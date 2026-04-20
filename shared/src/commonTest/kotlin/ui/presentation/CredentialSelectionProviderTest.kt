package ui.presentation

import at.asitplus.dcapi.OpenId4VpResponseSigned
import at.asitplus.dcapi.OpenId4VpResponseUnsigned
import at.asitplus.openid.AuthenticationResponseParameters
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.openid.AuthenticationResponseResult
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun serializesUnsignedDcApiResponsesWithoutTheOuterWrapper() {
        val serializedResponse = serializeDcApiPresentationResponse(
            AuthenticationResponseResult.DcApi(
                OpenId4VpResponseUnsigned(
                    data = AuthenticationResponseParameters(
                        state = "state",
                        vpToken = JsonPrimitive("vp-token"),
                    ),
                    origin = "https://wallet.example",
                )
            )
        )

        val parsedResponse = vckJsonSerializer.decodeFromString<AuthenticationResponseParameters>(serializedResponse)
        assertEquals("state", parsedResponse.state)
        assertEquals(JsonPrimitive("vp-token"), parsedResponse.vpToken)
        assertFalse(serializedResponse.contains("\"protocol\""))
        assertFalse(serializedResponse.contains("\"data\""))
    }

    @Test
    fun serializesEncryptedDcApiResponsesAsAuthenticationResponseParameters() {
        val serializedResponse = serializeDcApiPresentationResponse(
            AuthenticationResponseResult.DcApi(
                OpenId4VpResponseSigned(
                    data = AuthenticationResponseParameters(
                        response = "header.payload.signature.encrypted.tag",
                    ),
                    origin = "https://wallet.example",
                )
            )
        )

        val parsedResponse = vckJsonSerializer.decodeFromString<AuthenticationResponseParameters>(serializedResponse)
        assertEquals("header.payload.signature.encrypted.tag", parsedResponse.response)
        assertFalse(serializedResponse.contains("\"protocol\""))
        assertFalse(serializedResponse.contains("\"data\""))
    }
}
