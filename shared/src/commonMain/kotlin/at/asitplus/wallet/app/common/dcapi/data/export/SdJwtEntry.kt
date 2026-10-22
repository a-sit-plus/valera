package at.asitplus.wallet.app.common.dcapi.data.export

import at.asitplus.jsonpath.core.NormalizedJsonPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SdJwtEntry(
    @SerialName("documentId")
    val jwtId: String,
    @SerialName("vct")
    val verifiableCredentialType: String,
    // No serialization default: the version must be present even when encodeDefaults is false.
    val claimsFormatVersion: Int,
    val claimsTree: SdJwtClaimNode,
) {
    companion object {
        /** Exports reconstructed JSON without flattening names or inventing wildcard claims. */
        fun fromJson(
            jwtId: String,
            verifiableCredentialType: String,
            claims: JsonObject,
            attributeLabel: (NormalizedJsonPath) -> String?,
        ): SdJwtEntry = SdJwtEntry(
            jwtId = jwtId,
            verifiableCredentialType = verifiableCredentialType,
            claimsFormatVersion = 1,
            claimsTree = SdJwtClaimNode.fromJson(claims, attributeLabel),
        )
    }
}
