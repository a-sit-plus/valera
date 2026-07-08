package at.asitplus.wallet.app.common.relyingParty.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * ETSI TS 119 475 V1.2.1 Annex C claim selector.
 */
@Serializable
data class WrpClaimDto(
    @SerialName("path")
    val path: List<String>,

    @SerialName("values")
    val values: List<JsonElement>? = null
)