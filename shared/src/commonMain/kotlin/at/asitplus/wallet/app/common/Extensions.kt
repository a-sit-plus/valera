package at.asitplus.wallet.app.common

import androidx.compose.runtime.Composable
import at.asitplus.catchingUnwrapped
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.DocRequest
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.IndexSegment
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.openid.CredentialFormatEnum
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentIndex
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentString
import at.asitplus.openid.dcql.DCQLAmbiguousClaimsQuery
import at.asitplus.openid.dcql.DCQLClaimsPathPointerSegment
import at.asitplus.openid.dcql.DCQLCredentialQuery
import at.asitplus.openid.dcql.DCQLIsoMdocClaimsQuery
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialQuery
import at.asitplus.openid.dcql.DCQLIsoMdocZkCredentialQuery
import at.asitplus.openid.dcql.DCQLJsonClaimsQuery
import at.asitplus.openid.dcql.DCQLJwtVcCredentialQuery
import at.asitplus.openid.dcql.DCQLSdJwtCredentialQuery
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.*
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.VcDataModelConstants.VERIFIABLE_CREDENTIAL
import at.asitplus.wallet.lib.data.VcFallbackCredentialScheme
import at.asitplus.wallet.lib.oidvci.toFormat
import data.credentials.JsonClaimReference
import data.credentials.MdocClaimReference
import data.credentials.SingleClaimReference
import data.credentials.jwtClaimLabel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource
import ui.presentation.DCQLCredentialQueryUiModel
import ui.presentation.DCQLCredentialQueryUiModelAttributeLabels

typealias DcqlConsentData = Triple<CredentialRepresentation, CredentialScheme, Collection<SingleClaimReference?>?>

data class IsoDeviceRequestConsentData(
    val scheme: CredentialScheme,
    val attributes: List<NormalizedJsonPath>,
    /**
     * The elements the verifier declared it intends to keep beyond the transaction (ISO/IEC 18013-5
     * `IntentToRetain`), so consent can say so rather than implying a one-off read. Held as normalized path
     * strings because [NormalizedJsonPath] has no value equality; use [intendsToRetain] to query it.
     */
    val retainedAttributePaths: Set<String>,
) {
    fun intendsToRetain(path: NormalizedJsonPath) = path.toString() in retainedAttributePaths
}

/** Resolves one ISO document request and preserves namespace and element order. */
suspend fun DocRequest.extractConsentData(): IsoDeviceRequestConsentData {
    val request = itemsRequest.value
    val elements = request.namespaces.flatMap { (namespace, elements) ->
        elements.entries.map { element ->
            (NormalizedJsonPath() + namespace + element.dataElementIdentifier) to element.intentToRetain
        }
    }
    return IsoDeviceRequestConsentData(
        scheme = resolveConsentScheme(ISO_MDOC, listOf(request.docType)),
        attributes = elements.map { it.first },
        retainedAttributePaths = elements.filter { it.second }.mapTo(mutableSetOf()) { it.first.toString() },
    )
}

/** Resolves every ISO document request independently and preserves request, namespace, and element order. */
suspend fun DeviceRequest.extractConsentData(): List<IsoDeviceRequestConsentData> =
    docRequests.map { it.extractConsentData() }

/**
 * Resolves the first identifier yielding a scheme with known type metadata, triggering (cached)
 * remote type-metadata retrieval where needed. Unlike a lookup in [AttributeIndex.schemeSet],
 * this also works in a freshly started process whose in-memory scheme index is still cold
 * (e.g. the iOS identity provider extension answering a DC API request).
 */
private suspend fun resolveConsentScheme(
    representation: CredentialRepresentation,
    identifiers: Collection<String>,
): CredentialScheme {
    require(identifiers.isNotEmpty()) { "No credential identifier to resolve a scheme from" }
    val schemes = identifiers.map { AttributeIndex.resolveIdentifier(it, representation) }
    return schemes.firstOrNull { !it.isFallback() } ?: schemes.first()
}

private fun CredentialScheme.isFallback() = this is VcFallbackCredentialScheme
        || this is SdJwtFallbackCredentialScheme
        || this is IsoMdocFallbackCredentialScheme

/**
 * assumes json claim path pointers don't contain `null`, otherwise only the prefix is shown
 */
@Throws(Throwable::class)
suspend fun DCQLCredentialQuery.extractConsentData(): DcqlConsentData {
    val representation = when (format) {
        CredentialFormatEnum.DC_SD_JWT -> SD_JWT
        CredentialFormatEnum.MSO_MDOC -> ISO_MDOC
        else -> PLAIN_JWT
    }

    val scheme = when (this) {
        is DCQLIsoMdocCredentialQuery -> resolveConsentScheme(ISO_MDOC, listOf(meta.doctypeValue))

        is DCQLSdJwtCredentialQuery -> resolveConsentScheme(SD_JWT, meta.vctValues)

        is DCQLJwtVcCredentialQuery -> resolveConsentScheme(
            PLAIN_JWT,
            meta.typeValues.list.flatten().filterNot { it == VERIFIABLE_CREDENTIAL },
        )

        is DCQLIsoMdocZkCredentialQuery -> TODO()
    }

    // assuming all claims path pointers are single claim references
    val singleReferenceClaimsQueries = this.claims?.associateWith {
        when (it) {
            is DCQLJsonClaimsQuery -> JsonClaimReference(
                NormalizedJsonPath(it.path.map {
                    when (it) {
                        is DCQLClaimsPathPointerSegment.IndexSegment -> IndexSegment(it.index)
                        is DCQLClaimsPathPointerSegment.NameSegment -> NameSegment(it.name)
                        DCQLClaimsPathPointerSegment.NullSegment -> null
                    }
                }.takeWhile {
                    it != null
                }.filterNotNull())
            )

            is DCQLIsoMdocClaimsQuery -> MdocClaimReference(namespace = it.namespace, claimName = it.claimName)

            is DCQLAmbiguousClaimsQuery -> throw IllegalStateException("Unsupported claims query format: $it")
        }
    }
    return Triple(representation, scheme, singleReferenceClaimsQueries?.values)
}

private fun CredentialScheme.toCredentialClaimStructure(
    representation: CredentialRepresentation,
    requestedElements: Collection<String>? = null
): JsonElement {
    val claimPaths = claimPaths(requestedElements).let { paths ->
        when (representation) {
            PLAIN_JWT, SD_JWT -> paths
            ISO_MDOC -> paths.map { it.qualifiedWithIsoNamespace(isoNamespace) }
        }
    }

    return when (representation) {
        PLAIN_JWT -> buildJsonObject {
            putClaimPaths(claimPaths)
            put("type", vcType ?: "")
        }

        SD_JWT -> buildJsonObject {
            addSdJwtDummyMetadata()
            putClaimPaths(claimPaths)
            put("vct", sdJwtType ?: "")
        }

        ISO_MDOC -> JsonObject(emptyMap()).withClaimPaths(claimPaths)
    }
}

private sealed interface ClaimPathSegment {
    data class Name(val value: String) : ClaimPathSegment
    data class Index(val value: Int) : ClaimPathSegment
}

private fun CredentialScheme.claimPaths(requestedElements: Collection<String>?): List<List<ClaimPathSegment>> =
    claimDescriptions.map { it.path.toClaimPath() } +
            requestedElements.orEmpty().map { listOf(ClaimPathSegment.Name(it)) }

private fun OpenId4VciClaimsPathPointer.toClaimPath(): List<ClaimPathSegment> = mapNotNull {
    when (it) {
        is OpenId4VciClaimsPathPointerSegmentString -> ClaimPathSegment.Name(it.string)
        is OpenId4VciClaimsPathPointerSegmentIndex -> it.uint
            .takeIf { index -> index <= Int.MAX_VALUE.toUInt() }
            ?.let { index -> ClaimPathSegment.Index(index.toInt()) }

        null -> null
    }
}

private fun List<ClaimPathSegment>.qualifiedWithIsoNamespace(isoNamespace: String?): List<ClaimPathSegment> =
    if (isoNamespace == null || firstOrNull() == ClaimPathSegment.Name(isoNamespace)) this
    else listOf(ClaimPathSegment.Name(isoNamespace)) + this

private fun JsonObjectBuilder.putClaimPaths(paths: Collection<List<ClaimPathSegment>>) {
    withClaimPaths(paths).forEach { put(it.key, it.value) }
}

private fun JsonObjectBuilder.withClaimPaths(paths: Collection<List<ClaimPathSegment>>): JsonObject =
    JsonObject(emptyMap()).withClaimPaths(paths)

private fun JsonObject.withClaimPaths(paths: Collection<List<ClaimPathSegment>>): JsonObject =
    paths.fold(this as JsonElement) { current, path -> current.withClaimPath(path) } as JsonObject

private fun JsonElement.withClaimPath(path: List<ClaimPathSegment>): JsonElement {
    if (path.isEmpty()) return JsonPrimitive("")

    return when (val segment = path.first()) {
        is ClaimPathSegment.Name -> {
            val entries = (this as? JsonObject)?.toMutableMap() ?: mutableMapOf()
            entries[segment.value] = (entries[segment.value] ?: JsonObject(emptyMap())).withClaimPath(path.drop(1))
            JsonObject(entries)
        }

        is ClaimPathSegment.Index -> {
            val entries = (this as? JsonArray)?.toMutableList() ?: mutableListOf()
            while (entries.size <= segment.value) {
                entries.add(JsonNull)
            }
            entries[segment.value] = entries[segment.value].withClaimPath(path.drop(1))
            JsonArray(entries)
        }
    }
}

private fun JsonObjectBuilder.addSdJwtDummyMetadata() {
    put("iss", "")
    put("sub", "")
    put("nbf", 0)
    put("iat", 0)
    put("exp", 0)
    put("cnf", buildJsonObject { })
    put("status", buildJsonObject { })
}

fun Throwable.enrichMessage() = message ?: toString()

// Only NameSegments carry a member name; IndexSegments (e.g. [0] in array paths) are skipped.
fun NormalizedJsonPath.memberName(id: Int) =
    this.segments.filterIsInstance<NameSegment>().map { it.memberName }.getOrNull(id)

// Removes NameSegments whose name matches [name]; IndexSegments are passed through unchanged.
fun NormalizedJsonPath.minus(name: String) =
    NormalizedJsonPath(this.segments.filter { it !is NameSegment || it.memberName != name })

@Composable
fun Triple<CredentialRepresentation, CredentialScheme, Collection<SingleClaimReference?>?>.toCredentialQueryUiModel(): DCQLCredentialQueryUiModel {
    val (representation, scheme, attributePaths) = this
    return DCQLCredentialQueryUiModel(
        credentialRepresentationLocalized = representation.uiLabel(),
        credentialSchemeLocalized = scheme.uiLabel(),
        requestedAttributesLocalized = attributePaths?.let { claimReferences ->
            DCQLCredentialQueryUiModelAttributeLabels(
                otherAttributes = claimReferences.count { it == null },
                attributesLocalized = claimReferences.filterNotNull().map { path ->
                    catchingUnwrapped {
                        scheme.getLocalization(path)
                            ?: representation.getMetadataLocalization(path)?.let { stringResource(it) }
                            ?: path.displayPath()
                    }.getOrElse { path.displayPath() }
                }
            )
        },
    )
}

// Last-resort label when no metadata localization exists: render the claim path itself,
// not the value-class wrapper (path.toString() would print "JsonClaimReference(normalizedJsonPath=$['family_name'])").
fun SingleClaimReference.displayPath(): String = when (this) {
    is JsonClaimReference -> normalizedJsonPath.toShorthandNameSegmentNotationWherePossible().removePrefix("$.")
    is MdocClaimReference -> NormalizedJsonPath(
        NameSegment(namespace),
        NameSegment(claimName)
    ).toShorthandNameSegmentNotationWherePossible().removePrefix("$.")
}

fun ConstantIndex.CredentialRepresentation.getMetadataLocalization(
    claimReference: SingleClaimReference
) = when (claimReference) {
    is JsonClaimReference -> claimReference.normalizedJsonPath.segments.filterIsInstance<NameSegment>()
        .firstOrNull()
        ?.takeIf { this != ISO_MDOC }
        ?.let { jwtClaimLabel(it.memberName) }

    is MdocClaimReference -> null
}
