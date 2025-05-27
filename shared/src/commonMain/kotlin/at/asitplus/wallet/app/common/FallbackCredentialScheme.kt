package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.data.ConstantIndex

data class FallbackCredentialScheme(
    override val schemaUri: String = "https://wallet.a-sit.at/schemas/1.0.0/unknown.json",
    override val vcType: String? = null,
    override val sdJwtType: String? = null,
    override val isoNamespace: String? = null,
    override val isoDocType: String? = null,
    override val claimNames: Collection<String> = listOf(),
    override val supportedRepresentations: Collection<ConstantIndex.CredentialRepresentation> = setOf()
) : ConstantIndex.CredentialScheme {
    companion object {}
}