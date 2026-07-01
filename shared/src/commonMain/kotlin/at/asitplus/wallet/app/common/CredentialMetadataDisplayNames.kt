package at.asitplus.wallet.app.common

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * In-memory lookup of human-readable credential type names taken from resolved SD-JWT VC Type Metadata, keyed by the
 * resolved scheme's identifier (vct/docType) — the value the UI has at hand when rendering a stored credential.
 *
 * Schemes resolved from remote type metadata are [at.asitplus.wallet.lib.data.ExtractedSdJwtCredentialScheme] etc.,
 * which do not carry the type-level display name — so without this, such credentials would show their bare `vct`.
 * Populated by [PersistentCachingCredentialMetadataRegistry] as metadata is resolved.
 */
object CredentialMetadataDisplayNames {
    private val names = atomic(mapOf<String, String>())

    operator fun set(identifier: String, displayName: String) {
        names.update { if (it[identifier] == displayName) it else it + (identifier to displayName) }
    }

    operator fun get(identifier: String?): String? = identifier?.let { names.value[it] }
}
