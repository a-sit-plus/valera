package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.data.primitives.CacheStoreEntry
import at.asitplus.wallet.app.common.data.primitives.SimpleStore
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlin.io.encoding.Base64
import kotlin.time.Instant

/** Persists the JWT and CWT status-list tokens requested by Valera together with their cache timestamps. */
class PersistentStatusListTokenStore(
    dataStoreService: DataStoreService,
) : SimpleStore<UniformResourceIdentifier, CacheStoreEntry<StatusListToken>> {
    private val store = PersistentSimpleStore<String, StoredStatusListToken>(
        dataStoreService = dataStoreService,
        preferenceKey = Configuration.DATASTORE_KEY_TOKEN_STATUS_LIST_CACHE,
        maxEntries = Configuration.MAX_PERSISTENT_CACHE_ENTRIES,
    )

    override suspend fun get(key: UniformResourceIdentifier): CacheStoreEntry<StatusListToken>? =
        store[key.string]?.toCacheEntry(key)

    override suspend fun set(
        key: UniformResourceIdentifier,
        value: CacheStoreEntry<StatusListToken>,
    ): CacheStoreEntry<StatusListToken>? =
        store.set(key.string, value.toStored())?.toCacheEntry(key, removeIfInvalid = false)

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
            val resolvedAt = resolvedAtEpochMilliseconds?.let(Instant::fromEpochMilliseconds)
            val token: StatusListToken = when {
                compactJwt != null -> StatusListJwt(JwsCompactTyped<StatusListTokenPayload>(compactJwt), resolvedAt)
                cwt != null -> StatusListCwt(
                    CoseSigned.deserialize(ByteArraySerializer(), Base64.decode(cwt)).getOrThrow(),
                    resolvedAt,
                )

                else -> throw IllegalStateException("No status list token stored")
            }
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

    private fun CacheStoreEntry<StatusListToken>.toStored() = StoredStatusListToken(
        compactJwt = (data as? StatusListJwt)?.value?.toString(),
        cwt = (data as? StatusListCwt)?.let { Base64.encode(it.value.serialize(ByteArraySerializer())) },
        resolvedAtEpochMilliseconds = data.resolvedAt?.toEpochMilliseconds(),
        cachedAtEpochMilliseconds = createdTime.toEpochMilliseconds(),
    )
}

@Serializable
private data class StoredStatusListToken(
    val compactJwt: String? = null,
    /** The COSE_Sign1 of a CWT token, base64-encoded. */
    val cwt: String? = null,
    val resolvedAtEpochMilliseconds: Long?,
    val cachedAtEpochMilliseconds: Long,
)
