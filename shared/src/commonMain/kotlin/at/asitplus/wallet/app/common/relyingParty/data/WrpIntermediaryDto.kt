package at.asitplus.wallet.app.common.relyingParty.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ETSI TS 119 475 V1.2.1 Annex C intermediary reference.
 */
@Serializable
data class WrpIntermediaryDto(
    @SerialName("sub")
    val sub: String? = null,

    @SerialName("sname")
    val sname: String? = null
)