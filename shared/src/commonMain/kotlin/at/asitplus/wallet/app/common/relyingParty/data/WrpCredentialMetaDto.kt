package at.asitplus.wallet.app.common.relyingParty.data

import at.asitplus.data.NonEmptyList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ETSI TS 119 475 V1.2.1 Annex C credential metadata block.
 */
@Serializable
data class WrpCredentialMetaDto(
    @SerialName("doctype_value")
    val doctypeValue: String? = null,

    @SerialName("vct_values")
    val vctValues: NonEmptyList<String>? = null
) {
    fun toDomain() = when {
        doctypeValue != null -> WrpCredentialMeta.WrpDocType(doctypeValue)
        vctValues != null -> WrpCredentialMeta.WrpVctType(vctValues)
        else -> throw Throwable("WrpCredentialMetaDto empty")
    }
}

@Serializable
sealed interface WrpCredentialMeta {
    data class WrpDocType(
        @SerialName("doctype_value") val doctypeValue: String,
    ) : WrpCredentialMeta

    data class WrpVctType(
        @SerialName("vct_values") val vctValues: NonEmptyList<String>
    ) : WrpCredentialMeta
}