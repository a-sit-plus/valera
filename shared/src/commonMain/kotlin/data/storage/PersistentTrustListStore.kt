package data.storage

import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PersistentTrustListStore(
    private val dataStoreService: DataStoreService,
) {
    suspend fun persistTrustList(url: String, rawJwsText: String) =
        dataStoreService.setPreference(rawJwsText, url)


    fun observeTrustContainer(urls: List<String>): Flow<Map<String, ListOfTrustedEntities>> {
        val flows: List<Flow<Pair<String, ListOfTrustedEntities?>>> = urls.map { url ->
            dataStoreService.getPreference(url).map { rawJws ->
                url to parseLoTE(rawJws)
            }
        }

        return combine(flows) { array ->
            array.filter { it.second != null }
                .associate { it.first to it.second!! }
        }
    }

    private fun parseLoTE(rawJws: String?): ListOfTrustedEntities? {
        if (rawJws.isNullOrBlank()) return null
        return runCatching {
            JwsCompact.parse<TrustListPayload>(rawJws).getOrThrow().second.loTe
        }.getOrNull()
    }
}