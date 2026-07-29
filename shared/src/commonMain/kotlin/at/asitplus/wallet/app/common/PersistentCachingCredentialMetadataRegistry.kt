package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.CredentialMetadataRegistry
import at.asitplus.wallet.lib.data.ResolvedCredentialMetadata
import at.asitplus.wallet.lib.data.toCredentialScheme
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadata
import data.storage.DataStoreService
import data.storage.PersistentSimpleStore
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [CredentialMetadataRegistry] that persists resolved type-metadata to the local data store. Fresh entries avoid
 * the network, expired entries are refreshed through [delegate], and stale metadata remains available when refresh
 * fails.
 *
 * [AttributeIndex] only calls [findEntry] when a scheme is not already known synchronously, so wrapping the remote
 * registry here means remote calls happen exactly for schemes that are neither bundled, already resolved this
 * session, nor present in the persisted cache as a fresh entry.
 */
@OptIn(ExperimentalTime::class)
class PersistentCachingCredentialMetadataRegistry(
    private val delegate: CredentialMetadataRegistry,
    private val dataStore: DataStoreService,
    private val ttl: Duration = 7.days,
    private val clock: Clock = Clock.System,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : CredentialMetadataRegistry {

    @Serializable
    private data class CachedMetadata(
        val metadata: SdJwtTypeMetadata,
        val loadedFrom: String,
        val aliases: Set<String> = emptySet(),
        // Epoch seconds when this entry was cached; 0 (the default for pre-TTL entries) is always stale.
        val cachedAtEpochSeconds: Long = 0,
    )

    private fun CachedMetadata.isFresh(): Boolean =
        clock.now() - Instant.fromEpochSeconds(cachedAtEpochSeconds) < ttl

    private val cache = PersistentSimpleStore(
        dataStoreService = dataStore,
        preferenceKey = Configuration.DATASTORE_KEY_CREDENTIAL_METADATA_CACHE,
        keySerializer = String.serializer(),
        valueSerializer = CachedMetadata.serializer(),
        json = json,
        maxEntries = Configuration.MAX_PERSISTENT_CACHE_ENTRIES,
    )

    private fun key(identifier: String, representation: CredentialRepresentation) = "$representation|$identifier"

    override suspend fun findEntry(
        identifier: String,
        representation: CredentialRepresentation,
    ): ResolvedCredentialMetadata? {
        val key = key(identifier, representation)
        val cached = cache[key]
        cached?.takeIf { it.isFresh() }?.let {
            Napier.d("Type metadata cache hit for $identifier ($representation), loaded from ${it.loadedFrom}")
            return it.toResolved().also(::rememberDisplayName)
        }

        val resolved = delegate.findEntry(identifier, representation) ?: run {
            cached?.let {
                Napier.w("Could not refresh type metadata for $identifier ($representation); using stale cache")
                return it.toResolved().also(::rememberDisplayName)
            }
            // Resolution is otherwise swallowed to a fallback scheme (bare vct, no claim labels) with no trace.
            Napier.w("Could not resolve type metadata for $identifier ($representation); using fallback scheme")
            return null
        }
        Napier.d("Resolved type metadata for $identifier from ${resolved.loadedFrom} (name=${resolved.metadata.name})")
        rememberDisplayName(resolved)
        cache[key] = resolved.toCached()
        return resolved
    }

    /**
     * Record the credential type's display name so the UI can show it instead of the bare vct. Keyed by the resolved
     * scheme's identifier (vct/docType) — that is what the UI has at hand; the document URL (loadedFrom) is not.
     */
    private fun rememberDisplayName(resolved: ResolvedCredentialMetadata) {
        val name = resolved.metadata.name ?: return
        CredentialMetadataDisplayNames[resolved.toCredentialScheme().identifier] = name
    }

    private fun CachedMetadata.toResolved() = ResolvedCredentialMetadata(metadata, loadedFrom, aliases)
    private fun ResolvedCredentialMetadata.toCached() =
        CachedMetadata(metadata, loadedFrom, aliases, clock.now().epochSeconds)
}
