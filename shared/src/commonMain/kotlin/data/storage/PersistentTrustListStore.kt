package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.time.Instant
import kotlin.time.measureTimedValue

class PersistentTrustListStore(
    private val dataStoreService: DataStoreService,
) {
    suspend fun persistTrustList(url: String, rawJwsText: String, cachedAt: Instant) =
        dataStoreService.setPreference(
            joseCompliantSerializer.encodeToString(StoredTrustList(rawJwsText, cachedAt.toEpochMilliseconds())),
            url,
        )

    /** Reads only the time the list was cached, without parsing the list itself. */
    suspend fun getCachedAt(url: String): Instant? =
        dataStoreService.getPreference(url).first()?.let(::decodeStored)?.second

    /**
     * Drops the persisted list of [url], e.g. because the stage publishing it was disabled.
     * Returns `true` if there was something to remove.
     */
    suspend fun removeTrustList(url: String): Boolean = dataStoreService.deletePreferenceIfPresent(url)

    /** Emits each URL's trusted-entity list together with the time it was cached, for offline-TTL enforcement. */
    fun observeTrustContainer(urls: List<String>): Flow<Map<String, Pair<ListOfTrustedEntities, Instant>>> {
        // `combine` of no flows never emits, which would leave every observer waiting forever once
        // the user disabled all trust list stages.
        if (urls.isEmpty()) return flowOf(emptyMap())

        // The preferences emit on every change of any key, e.g. of the HTTP cache, so a list is parsed only if its
        // own value changed, and then only once per process, see [ParsedTrustLists]
        val flows: List<Flow<Pair<String, Pair<ListOfTrustedEntities, Instant>?>>> = urls.map { url ->
            dataStoreService.getPreference(url).distinctUntilChanged().map { stored ->
                url to ParsedTrustLists.parse(url, stored)
            }
        }

        // Parsing the lists is CPU-intensive, and the trust states collected by the UI observe them
        return combine(flows) { array ->
            array.mapNotNull { (url, entry) -> entry?.let { url to it } }.toMap()
        }.flowOn(Dispatchers.Default)
    }
}

/**
 * The lists of this process, parsed from their persisted values. Every trust state shown and every anchor lookup
 * observes all lists, and parsing each of them for every observer at the same time exceeded the memory limit of the
 * iOS Identity Document Provider extension, which the system then terminated. Lists are parsed one at a time, and
 * only if the persisted value differs from the one parsed last, e.g. because the other process refreshed it.
 */
private object ParsedTrustLists {
    private val mutex = Mutex()
    private val parsed = mutableMapOf<String, Pair<String, Pair<ListOfTrustedEntities, Instant>?>>()

    suspend fun parse(url: String, stored: String?): Pair<ListOfTrustedEntities, Instant>? = mutex.withLock {
        if (stored.isNullOrBlank()) {
            parsed.remove(url)
            return@withLock null
        }
        parsed[url]?.takeIf { (parsedFrom, _) -> parsedFrom == stored }?.let { return@withLock it.second }
        measureTimedValue { parseStored(stored) }.let { (list, duration) ->
            Napier.d("Timing: parsed Trust List $url (${stored.length} characters) in $duration")
            list
        }.also { parsed[url] = stored to it }
    }
}

/** The raw JWS and the time it was cached of a persisted value. */
private fun decodeStored(stored: String): Pair<String, Instant>? {
    if (stored.isBlank()) return null
    return catchingUnwrapped {
        val dto = joseCompliantSerializer.decodeFromString<StoredTrustList>(stored)
        dto.rawJwsText to Instant.fromEpochMilliseconds(dto.cachedAtEpochMillis)
    }.getOrElse {
        // Legacy value (raw JWS, no timestamp): treat as epoch 0 so the offline-TTL forces a refetch.
        stored to Instant.fromEpochMilliseconds(0)
    }
}

private fun parseStored(stored: String): Pair<ListOfTrustedEntities, Instant>? {
    val (rawJws, cachedAt) = decodeStored(stored) ?: return null
    val loTe = parseLoTE(rawJws) ?: return null
    return loTe to cachedAt
}

private fun parseLoTE(rawJws: String): ListOfTrustedEntities? = runCatching {
    JwsCompact.parse<TrustListPayload>(rawJws).getOrThrow().second.loTe
}.getOrNull()

@Serializable
private data class StoredTrustList(val rawJwsText: String, val cachedAtEpochMillis: Long)
