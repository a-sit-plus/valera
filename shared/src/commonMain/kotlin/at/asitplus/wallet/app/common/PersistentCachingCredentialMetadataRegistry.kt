package at.asitplus.wallet.app.common

import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.CredentialMetadataRegistry
import at.asitplus.wallet.lib.data.ResolvedCredentialMetadata
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadata
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [CredentialMetadataRegistry] that persists resolved type-metadata to the local data store, so a credential's
 * scheme is fetched from the network only once: on the first lookup it delegates to [delegate] (e.g. the remote
 * registry) and caches the result; on every later lookup — including after an app restart — it answers from the
 * persisted cache without any remote call.
 *
 * [AttributeIndex] only calls [findEntry] when a scheme is not already known synchronously, so wrapping the remote
 * registry here means remote calls happen exactly for schemes that are neither bundled, already resolved this
 * session, nor present in the persisted cache.
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

    private val mutex = Mutex()
    private var cache: MutableMap<String, CachedMetadata>? = null

    private fun key(identifier: String, representation: CredentialRepresentation) = "$representation|$identifier"

    private suspend fun cache(): MutableMap<String, CachedMetadata> = mutex.withLock {
        cache ?: loadFromDataStore().also { cache = it }
    }

    private suspend fun loadFromDataStore(): MutableMap<String, CachedMetadata> = catchingUnwrapped {
        dataStore.getPreference(Configuration.DATASTORE_KEY_CREDENTIAL_METADATA_CACHE).first()
            ?.let { json.decodeFromString<Map<String, CachedMetadata>>(it).toMutableMap() }
    }.getOrNull() ?: mutableMapOf()

    override suspend fun findEntry(
        identifier: String,
        representation: CredentialRepresentation,
    ): ResolvedCredentialMetadata? {
        val key = key(identifier, representation)
        cache()[key]?.takeIf { it.isFresh() }?.let {
            return it.toResolved().also(::rememberDisplayName)
        }

        val resolved = delegate.findEntry(identifier, representation) ?: return null
        rememberDisplayName(resolved)
        mutex.withLock {
            val current = cache ?: loadFromDataStore().also { cache = it }
            current[key] = resolved.toCached()
            catchingUnwrapped {
                dataStore.setPreference(
                    json.encodeToString<Map<String, CachedMetadata>>(current),
                    Configuration.DATASTORE_KEY_CREDENTIAL_METADATA_CACHE,
                )
            }.onFailure { Napier.w("Could not persist credential metadata cache", it) }
        }
        return resolved
    }

    /** Record the credential type's display name so the UI can show it instead of the bare vct. */
    private fun rememberDisplayName(resolved: ResolvedCredentialMetadata) {
        resolved.metadata.name?.let { CredentialMetadataDisplayNames[resolved.loadedFrom] = it }
    }

    private fun CachedMetadata.toResolved() = ResolvedCredentialMetadata(metadata, loadedFrom, aliases)
    private fun ResolvedCredentialMetadata.toCached() =
        CachedMetadata(metadata, loadedFrom, aliases, clock.now().epochSeconds)
}
