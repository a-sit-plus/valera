package data.credentials

import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.openid.OpenId4VciClaimsPathPointerSegmentString
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.mdl.MDL_DOCTYPE

object SelectableCredentialSchemes {
    val mdl: CredentialScheme get() = schemeForIsoDocType(MDL_DOCTYPE)
    val euPid: CredentialScheme get() = schemeForIsoDocType(EU_PID_DOCTYPE)
    val ageVerification: CredentialScheme get() = schemeForIsoDocType(AV_DOC_TYPE)

    val schemes: List<CredentialScheme>
        get() = listOf(mdl, euPid, ageVerification)
}

/** Resolve a metadata-backed scheme by ISO docType, falling back to an unknown scheme (namespace == docType). */
fun schemeForIsoDocType(docType: String): CredentialScheme =
    AttributeIndex.resolveIsoDoctype(docType) ?: IsoMdocFallbackCredentialScheme(isoDocType = docType)

fun OpenId4VciClaimsPathPointer.toIsoElementIdentifier(isoNamespace: String): String? {
    val segments = mapNotNull { (it as? OpenId4VciClaimsPathPointerSegmentString)?.string }
    val elementSegments = if (segments.firstOrNull() == isoNamespace) segments.drop(1) else segments
    return elementSegments.takeIf { it.isNotEmpty() }?.joinToString(".")
}

fun CredentialScheme.mandatoryClaimPaths(): List<OpenId4VciClaimsPathPointer> =
    claimDescriptions.filter { it.mandatory == true }.map { it.path }

fun CredentialScheme.resolveClaimPath(elementIdentifier: String): OpenId4VciClaimsPathPointer =
    claimDescriptions.firstOrNull { claim ->
        claim.path.toIsoElementIdentifier(isoNamespace ?: "") == elementIdentifier
    }?.path ?: OpenId4VciClaimsPathPointer(elementIdentifier)
