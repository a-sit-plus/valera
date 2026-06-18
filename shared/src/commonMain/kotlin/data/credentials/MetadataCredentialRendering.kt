package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentString
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
fun CredentialScheme.metadataLabel(path: NormalizedJsonPath, locale: String = "en"): String? {
    val key = path.toString()
    val claim = claimDescriptions.firstOrNull {
        it.path.toNormalizedJsonPathOrNull()?.toString() == key
    } ?: return null
    val displays = claim.display ?: return null
    return (displays.firstOrNull { it.locale?.startsWith(locale) == true } ?: displays.firstOrNull())?.name
}
