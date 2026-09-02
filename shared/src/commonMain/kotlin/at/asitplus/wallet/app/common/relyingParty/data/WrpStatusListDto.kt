package at.asitplus.wallet.app.common.relyingParty.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ETSI TS 119 475 V1.2.1 Annex C status-list reference.
 */
@Serializable
data class WrpStatusListDto(
    @SerialName("idx")
    val idx: Int,

    @SerialName("uri")
    val uri: String
)