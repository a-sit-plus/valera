package ui.models

import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.VcDataModelConstants.VERIFIABLE_CREDENTIAL
import at.asitplus.wallet.lib.data.VcFallbackCredentialScheme

/**
 * UI-facing credential model with the suspend-resolved scheme attached.
 *
 * VC-K keeps scheme resolution behind [StoreEntry.resolveScheme], so composables and other
 * synchronous UI code should use this wrapper instead of the deprecated StoreEntry scheme fields.
 */
data class ResolvedCredential(
    val entry: StoreEntry,
    val scheme: CredentialScheme,
)

suspend fun StoreEntry.toResolvedCredential() = ResolvedCredential(
    entry = this,
    scheme = resolveScheme(),
)

fun StoreEntry.toFallbackResolvedCredential() = ResolvedCredential(
    entry = this,
    scheme = fallbackScheme(),
)

/**
 * Mirrors VC-K's legacy fallback scheme behavior without reading deprecated StoreEntry.scheme.
 */
private fun StoreEntry.fallbackScheme(): CredentialScheme = when (this) {
    is StoreEntry.Vc ->
        VcFallbackCredentialScheme(vc.vc.type.first { it != VERIFIABLE_CREDENTIAL })

    is StoreEntry.SdJwt ->
        SdJwtFallbackCredentialScheme(sdJwt.verifiableCredentialType)

    is StoreEntry.Iso -> issuerSigned.issuerAuth.payload?.docType
        ?.let { IsoMdocFallbackCredentialScheme(it) }
        ?: IsoMdocFallbackCredentialScheme("unknown")
}
