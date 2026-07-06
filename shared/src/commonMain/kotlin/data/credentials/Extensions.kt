package data.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPid
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialScheme
import data.Attribute
import data.credentials.CredentialAdapter.Companion.toComplexJson
import data.credentials.CredentialAdapter.Companion.toNamespaceAttributeMap
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

@Suppress("DEPRECATION")
@Composable
fun SubjectCredentialStore.StoreEntry.toCredentialAdapter(
    scheme: CredentialScheme,
    decodeImage: (ByteArray) -> Result<ImageBitmap>,
): CredentialAdapter = scheme.let { s ->
    when {
        s.isEuPid -> EuPidCredentialAdapter.createFromStoreEntry(this, scheme, decodePortrait = decodeImage)
        s.isMdl -> MobileDrivingLicenceCredentialAdapter.createFromStoreEntry(this, scheme, decodePortrait = decodeImage)
        // No bespoke renderer (e.g. a scheme resolved from remote type metadata): render generically.
        else -> FallbackCredentialAdapter(toGenericAttributeList(), this, scheme)
    }
}


class FallbackCredentialAdapter(
    genericAttributeList: List<Pair<NormalizedJsonPath, Any>>,
    val credential: SubjectCredentialStore.StoreEntry,
    override val scheme: CredentialScheme,
) : CredentialAdapter() {
    // trying our best to map the values to attributes
    private val mapping = genericAttributeList.toMap()

    override fun getAttribute(path: NormalizedJsonPath): Attribute? = mapping[path]
        ?.let { Attribute.fromValue(it) }

    override val representation = credential.representation
}

/**
 * Labels `(path, value)` pairs for display in presentation cards: formats values through the bespoke
 * adapter where possible (raw value otherwise), labels from type-metadata claim descriptions where
 * available (raw claim path otherwise). Never drops a claim just because its scheme is unknown;
 * hides technical JWT claims like the credential details view does.
 */
fun CredentialAdapter.labeledPresentationAttributes(
    attributes: List<Pair<NormalizedJsonPath, Any>>,
): List<Pair<String, Attribute>> = attributes
    .filterNot { (path, _) -> path.memberName(0) in HIDDEN_TOP_LEVEL_CLAIMS }
    .mapNotNull { (path, value) ->
        val attribute = catchingUnwrapped { getAttribute(path) }.getOrNull()
            ?: Attribute.fromValue(value)
            ?: return@mapNotNull null
        val label = scheme.getLocalization(path)
            ?: path.segments.lastOrNull()?.let { scheme.getLocalization(NormalizedJsonPath(it)) }
            ?: path.genericLabel()
        label to attribute
    }
    .sortedBy { it.first }

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
