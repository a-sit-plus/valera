package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.dcapi.request.ExchangeProtocolIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class CredentialRegistry private constructor(
    @SerialName("protocols")
    val protocols: List<String>,
    @SerialName("credentials")
    val credentials: List<CredentialEntry>
) {
    companion object {
        fun create(credentials: List<CredentialEntry>): CredentialRegistry = CredentialRegistry(
            listOf(
                ExchangeProtocolIdentifier.OPENID4VP_V1_SIGNED,
                ExchangeProtocolIdentifier.OPENID4VP_V1_MULTISIGNED,
                ExchangeProtocolIdentifier.OPENID4VP_V1_UNSIGNED,
                ExchangeProtocolIdentifier.ORG_ISO_MDOC,
            ), credentials
        )
    }
}
