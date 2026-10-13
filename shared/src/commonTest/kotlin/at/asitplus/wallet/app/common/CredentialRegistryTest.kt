package at.asitplus.wallet.app.common

import at.asitplus.dcapi.request.ExchangeProtocolIdentifier
import at.asitplus.wallet.app.common.dcapi.data.export.CredentialRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialRegistryTest {
    @Test
    fun advertisesAllSupportedPresentationProtocols() {
        assertEquals(
            listOf(
                ExchangeProtocolIdentifier.OPENID4VP_V1_SIGNED,
                ExchangeProtocolIdentifier.OPENID4VP_V1_MULTISIGNED,
                ExchangeProtocolIdentifier.OPENID4VP_V1_UNSIGNED,
                ExchangeProtocolIdentifier.ORG_ISO_MDOC,
            ),
            CredentialRegistry.create(emptyList()).protocols,
        )
    }
}
