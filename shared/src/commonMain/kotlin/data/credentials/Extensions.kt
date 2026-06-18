package data.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ageverification.AgeVerificationScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.taxid.TaxIdScheme
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toComplexJson
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

@Suppress("DEPRECATION")
@Composable
fun SubjectCredentialStore.StoreEntry.toCredentialAdapter(
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
): CredentialAdapter? = scheme.let { s ->
    when {
        s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
        s.isMdl -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, decodePortrait = decodeImage)
        s is CertificateOfResidenceScheme -> CertificateOfResidenceCredentialAdapter.createFromStoreEntry(this)
        s is CompanyRegistrationScheme -> CompanyRegistrationCredentialAdapter.createFromStoreEntry(this)
        s is EhicScheme -> EhicCredentialAdapter.createFromStoreEntry(this)
        s is HealthIdScheme -> HealthIdCredentialAdapter.createFromStoreEntry(this)
        s is AgeVerificationScheme -> AgeVerificationCredentialAdapter.createFromStoreEntry(this)
        s is PowerOfRepresentationScheme -> PowerOfRepresentationCredentialAdapter.createFromStoreEntry(this)
        s is TaxIdScheme -> TaxIdCredentialAdapter.createFromStoreEntry(this)
        // No bespoke renderer (e.g. a scheme resolved from remote type metadata): render generically.
        else -> FallbackCredentialAdapter(toGenericAttributeList(), this)
    }
}


class FallbackCredentialAdapter(
    genericAttributeList: List<Pair<NormalizedJsonPath, Any>>,
    val credential: SubjectCredentialStore.StoreEntry
) : CredentialAdapter() {
    // trying our best to map the values to attributes
    private val mapping = genericAttributeList.toMap()

    /** All claim paths actually present in this credential (for metadata-driven generic rendering). */
    val attributePaths: List<NormalizedJsonPath> get() = mapping.keys.toList()

    override fun getAttribute(path: NormalizedJsonPath): Attribute? = mapping[path]
        ?.let { Attribute.fromValue(it) }

    override val representation = credential.representation
    override val scheme = credential.scheme!!
}

/**
 * Flattens all disclosed claims of a credential into `(path, value)` pairs, using the same path convention as
 * SD-JWT VC Type Metadata (nested SD-JWT objects become multi-segment paths; ISO mdoc claims become
 * `[namespace, element]`). Used to render credentials that have no bespoke adapter.
 */
fun SubjectCredentialStore.StoreEntry.toGenericAttributeList(): List<Pair<NormalizedJsonPath, Any>> =
    when (this) {
        is SubjectCredentialStore.StoreEntry.SdJwt ->
            toComplexJson()?.flattenToAttributePaths() ?: emptyList()

        is SubjectCredentialStore.StoreEntry.Iso ->
            toNamespaceAttributeMap()?.flatMap { (namespace, elements) ->
                elements.mapNotNull { (id, value) ->
                    value?.let { (NormalizedJsonPath() + namespace + id) to it }
                }
            } ?: emptyList()

        is SubjectCredentialStore.StoreEntry.Vc -> emptyList()
    }

private fun JsonObject.flattenToAttributePaths(
    prefix: NormalizedJsonPath = NormalizedJsonPath(),
): List<Pair<NormalizedJsonPath, Any>> = entries.flatMap { (key, value) ->
    val path = prefix + key
    when (value) {
        is JsonObject -> value.flattenToAttributePaths(path)
        is JsonNull -> emptyList()
        else -> listOf(path to value)
    }
}
