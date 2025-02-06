package data

import at.asitplus.catching
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.oidvci.WalletService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class RequestOptionParameters(
    val credentialIdentifier: String,
    val attributes: Set<String>?,
) {
    val resolved by lazy { AttributeIndex.resolveCredential(credentialIdentifier) }

    fun toRequestOptions(): WalletService.RequestOptions =
        resolved?.let { (scheme, representation) ->
            representation?.let { repr ->
                WalletService.RequestOptions(
                    credentialScheme = scheme, representation = repr, requestedAttributes = attributes
                )
            }
        } ?: throw Exception("Invalid RequestOptionParameters $this")

    fun serialize(): String = vckJsonSerializer.encodeToString(this)

    companion object {
        fun deserialize(input: String) = catching { vckJsonSerializer.decodeFromString<RequestOptionParameters>(input) }
    }
}