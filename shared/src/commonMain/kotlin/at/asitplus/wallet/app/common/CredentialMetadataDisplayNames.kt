package at.asitplus.wallet.app.common

import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.wallet.app.common.CredentialMetadataDisplayNames.get

/**
 * In-memory lookup of human-readable credential type names taken from resolved SD-JWT VC Type Metadata, keyed by the
 * resolved scheme's identifier (vct/docType) — the value the UI has at hand when rendering a stored credential.
 *
 * Schemes resolved from remote type metadata are [at.asitplus.wallet.lib.data.ExtractedSdJwtCredentialScheme] etc.,
 * which do not carry the type-level display name — so without this, such credentials would show their bare `vct`.
 * Populated by [PersistentCachingCredentialMetadataRegistry] as metadata is resolved.
 *
 * Backed by a [SnapshotStateMap] so a [get] made during composition recomposes once the name is resolved (metadata is
 * fetched asynchronously, so the value often arrives after the first render). Thread-safe for the background writes.
 */
object CredentialMetadataDisplayNames {
    private val names = SnapshotStateMap<String, String>()

    operator fun set(identifier: String, displayName: String) {
        names[identifier] = displayName
    }

    operator fun get(identifier: String?): String? = identifier?.let { names[it] }
}
