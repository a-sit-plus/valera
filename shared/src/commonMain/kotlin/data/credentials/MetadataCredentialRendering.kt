package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentString
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.lib.data.CredentialScheme

// Helpers to drive a generic credential UI from SD-JWT VC Type Metadata claim descriptions, for schemes that have
// no bespoke renderer (e.g. resolved from remote type metadata into an Extracted*CredentialScheme).

/** The metadata claim path as a [NormalizedJsonPath] using only its string segments, or null if it has none. */
fun OpenId4VciClaimsPathPointer.toNormalizedJsonPathOrNull(): NormalizedJsonPath? {
    val names = mapNotNull { (it as? OpenId4VciClaimsPathPointerSegmentString)?.string }
    if (names.isEmpty()) return null
    return names.fold(NormalizedJsonPath()) { acc, segment -> acc + segment }
}

/** Localized display label for [path] from this scheme's type-metadata claim descriptions, or null. */
fun CredentialScheme.metadataLabel(path: NormalizedJsonPath, locale: String = "en"): String? =
    metadataLabelCandidatePaths(path).firstNotNullOfOrNull { metadataLabelExact(it, locale) }

/**
 * Last-resort claim label when no metadata localization exists: the dot-joined claim path.
 *
 * An mdoc data element is addressed as `[namespace, elementIdentifier]`. The namespace carries no information for a
 * reader and makes an element that has no type-metadata entry look unlike the declared ones next to it: a requested
 * `age_over_23`, which an issuer need not provision and which is resolved per ISO/IEC 18013-5, 7.2.5, would read as
 * `org.iso.18013.5.1.age_over_23` while `age_over_21` right above it reads as `age_over_21`. Show the element
 * identifier alone for those, so declared and undeclared elements are labelled the same way.
 */
fun NormalizedJsonPath.genericLabel(): String {
    val names = (0 until size).mapNotNull { memberName(it) }
    return when {
        names.isEmpty() -> toString()
        // A dot in the first segment marks an mdoc namespace; SD-JWT paths like [address, formatted] keep both.
        names.size == 2 && '.' in names.first() -> names.last()
        else -> names.joinToString(".")
    }
}

/** Registered JWT claims rendered in dedicated cards (or not at all) rather than in attribute lists. */
val HIDDEN_TOP_LEVEL_CLAIMS = setOf("status", "cnf", "vct", "iat", "iss", "nbf", "exp", "sub")

private fun CredentialScheme.metadataLabelExact(path: NormalizedJsonPath, locale: String): String? {
    val key = path.toString()
    val claim = claimDescriptions.firstOrNull {
        it.path.toNormalizedJsonPathOrNull()?.toString() == key
    } ?: return null
    val displays = claim.display ?: return null
    return (displays.firstOrNull { it.locale?.startsWith(locale) == true } ?: displays.firstOrNull())?.name
}

private fun CredentialScheme.metadataLabelCandidatePaths(path: NormalizedJsonPath): List<NormalizedJsonPath> {
    val expanded = path.expandSingleDottedName()
    return buildList {
        add(path)
        if (expanded != path) add(expanded)
        isoNamespace?.takeIf { expanded.memberName(0) != it }?.let { namespace ->
            add(NormalizedJsonPath() + namespace + expanded)
        }
    }
}

private fun NormalizedJsonPath.expandSingleDottedName(): NormalizedJsonPath =
    memberName(0)
        ?.takeIf { size == 1 && "." in it }
        ?.split(".")
        ?.fold(NormalizedJsonPath()) { path, segment -> path + segment }
        ?: this