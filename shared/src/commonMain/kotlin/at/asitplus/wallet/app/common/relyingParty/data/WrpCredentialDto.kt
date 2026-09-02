package at.asitplus.wallet.app.common.relyingParty.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ETSI TS 119 475 V1.2.1 Annex C credential descriptor.
 */
@Serializable
data class WrpCredentialDto(
    @SerialName("format")
    val format: String,

    @SerialName("meta")
    val meta: WrpCredentialMetaDto,

    @SerialName("claim")
    val claim: List<WrpClaimDto> = emptyList()
)