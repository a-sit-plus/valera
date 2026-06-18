package at.asitplus.wallet.app.common

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * In-memory lookup of human-readable credential type names taken from resolved SD-JWT VC Type Metadata, keyed by the
 * metadata document URL (i.e. the resolved scheme's `schemaUri`).
 *
 * Schemes resolved from remote type metadata are [at.asitplus.wallet.lib.data.ExtractedSdJwtCredentialScheme] etc.,
 * which do not carry the type-level display name — so without this, such credentials would show their bare `vct`.
 * Populated by [PersistentCachingCredentialMetadataRegistry] as metadata is resolved.
 */
object CredentialMetadataDisplayNames {
    private val names = atomic(mapOf<String, String>())

    operator fun set(schemaUri: String, displayName: String) {
        names.update { if (it[schemaUri] == displayName) it else it + (schemaUri to displayName) }
    }

    operator fun get(schemaUri: String?): String? = schemaUri?.let { names.value[it] }
}
