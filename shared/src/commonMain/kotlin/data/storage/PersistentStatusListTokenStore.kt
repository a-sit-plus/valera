package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.data.primitives.CacheStoreEntry
import at.asitplus.wallet.app.common.data.primitives.SimpleStore
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.time.Instant

/** Persists the JWT status-list tokens requested by Valera together with their cache timestamps. */
class PersistentStatusListTokenStore(
    dataStoreService: DataStoreService,
) : SimpleStore<UniformResourceIdentifier, CacheStoreEntry<StatusListToken>> {
    private val store = PersistentSimpleStore(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_TOKEN_STATUS_LIST_CACHE,
        keySerializer = String.serializer(),
        valueSerializer = StoredStatusListToken.serializer(),
        maxEntries = Configuration.MAX_PERSISTENT_CACHE_ENTRIES,
    )

    override suspend fun get(key: UniformResourceIdentifier): CacheStoreEntry<StatusListToken>? =
        store[key.string]?.toCacheEntry(key)

    override suspend fun set(
        key: UniformResourceIdentifier,
        value: CacheStoreEntry<StatusListToken>,
    ): CacheStoreEntry<StatusListToken>? {
        val stored = value.toStoredOrNull() ?: run {
            Napier.w("Only JWT status-list tokens can be persisted; not caching $key")
            return null
        }
        return store.set(key.string, stored)?.toCacheEntry(key, removeIfInvalid = false)
    }

    // Not on the hot path: the bulk-store chain drives get()/put() directly. Kept for interface conformance.
    override suspend fun getOrPut(
        key: UniformResourceIdentifier,
        defaultValue: suspend () -> CacheStoreEntry<StatusListToken>,
    ): CacheStoreEntry<StatusListToken> = get(key) ?: defaultValue().also { set(key, it) }

    override suspend fun keys(): Set<UniformResourceIdentifier> =
        store.keys().mapTo(mutableSetOf(), ::UniformResourceIdentifier)

    override suspend fun entries(): Map<UniformResourceIdentifier, CacheStoreEntry<StatusListToken>> =
        store.entries().mapNotNull { (key, value) ->
            val uri = UniformResourceIdentifier(key)
            value.toCacheEntry(uri)?.let { uri to it }
        }.toMap()

    override suspend fun remove(key: UniformResourceIdentifier): CacheStoreEntry<StatusListToken>? =
        store.remove(key.string)?.toCacheEntry(key, removeIfInvalid = false)

    override suspend fun removeAllEntries() = store.removeAllEntries()

    private suspend fun StoredStatusListToken.toCacheEntry(
        key: UniformResourceIdentifier,
        removeIfInvalid: Boolean = true,
    ): CacheStoreEntry<StatusListToken>? {
        val parsed = catchingUnwrapped {
            val token: StatusListToken = StatusListJwt(
                value = JwsCompactTyped<StatusListTokenPayload>(compactJwt),
                resolvedAt = resolvedAtEpochMilliseconds?.let(Instant::fromEpochMilliseconds),
            )
            CacheStoreEntry(
                data = token,
                createdTime = Instant.fromEpochMilliseconds(cachedAtEpochMilliseconds),
            )
        }.onFailure {
            Napier.w("Ignoring invalid cached status list token for $key", it)
        }.getOrNull()
        if (parsed == null && removeIfInvalid) {
            store.remove(key.string)
        }
        return parsed
    }

    // ponytail: JWT-only persistence; CWT status-list tokens simply aren't cached (null skips the write).
    private fun CacheStoreEntry<StatusListToken>.toStoredOrNull(): StoredStatusListToken? {
        val jwt = data as? StatusListJwt ?: return null
        return StoredStatusListToken(
            compactJwt = jwt.value.toString(),
            resolvedAtEpochMilliseconds = jwt.resolvedAt?.toEpochMilliseconds(),
            cachedAtEpochMilliseconds = createdTime.toEpochMilliseconds(),
        )
    }
}

@Serializable
private data class StoredStatusListToken(
    val compactJwt: String,
    val resolvedAtEpochMilliseconds: Long?,
    val cachedAtEpochMilliseconds: Long,
)
