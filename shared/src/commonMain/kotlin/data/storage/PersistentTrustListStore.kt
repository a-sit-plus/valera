package data.storage

import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.time.Instant

class PersistentTrustListStore(
    private val dataStoreService: DataStoreService,
) {
    suspend fun persistTrustList(url: String, rawJwsText: String, cachedAt: Instant) =
        dataStoreService.setPreference(
            joseCompliantSerializer.encodeToString(StoredTrustList(rawJwsText, cachedAt.toEpochMilliseconds())),
            url,
        )

    suspend fun getCachedAt(url: String): Instant? =
        dataStoreService.getPreference(url).first()?.let(::parseStored)?.second

    /** Emits each URL's trusted-entity list together with the time it was cached, for offline-TTL enforcement. */
    fun observeTrustContainer(urls: List<String>): Flow<Map<String, Pair<ListOfTrustedEntities, Instant>>> {
        val flows: List<Flow<Pair<String, Pair<ListOfTrustedEntities, Instant>?>>> = urls.map { url ->
            dataStoreService.getPreference(url).map { stored ->
                url to parseStored(stored)
            }
        }

        return combine(flows) { array ->
            array.mapNotNull { (url, entry) -> entry?.let { url to it } }.toMap()
        }
    }

    private fun parseStored(stored: String?): Pair<ListOfTrustedEntities, Instant>? {
        if (stored.isNullOrBlank()) return null
        val (rawJws, cachedAt) = runCatching {
            val dto = joseCompliantSerializer.decodeFromString<StoredTrustList>(stored)
            dto.rawJwsText to Instant.fromEpochMilliseconds(dto.cachedAtEpochMillis)
        }.getOrElse {
            // Legacy value (raw JWS, no timestamp): treat as epoch 0 so the offline-TTL forces a refetch.
            stored to Instant.fromEpochMilliseconds(0)
        }
        val loTe = parseLoTE(rawJws) ?: return null
        return loTe to cachedAt
    }

    private fun parseLoTE(rawJws: String): ListOfTrustedEntities? = runCatching {
        JwsCompact.parse<TrustListPayload>(rawJws).getOrThrow().second.loTe
    }.getOrNull()
}

@Serializable
private data class StoredTrustList(val rawJwsText: String, val cachedAtEpochMillis: Long)
