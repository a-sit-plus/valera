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

object TrustListStorageKeys {
    private const val PREFIX = "lote_cache_"

    val PID_PROVIDERS = "${PREFIX}pid_providers"
    val WALLET_PROVIDERS = "${PREFIX}wallet_providers"
    val WRPAC_PROVIDERS = "${PREFIX}wrpac_providers"
    val MDL_PROVIDERS = "${PREFIX}mdl_providers"
    val PUB_EAA_PROVIDERS = "${PREFIX}pub_eaa_providers"
    val ASIT_PLUS_PROVIDERS = "${PREFIX}asit_plus_providers"


    fun mapUrlToKey(url: String): String? = when {
        url.contains("pid-providers") -> PID_PROVIDERS
        url.contains("wallet-providers") -> WALLET_PROVIDERS
        url.contains("wrpac-providers") -> WRPAC_PROVIDERS
        url.contains("mdl-providers") -> MDL_PROVIDERS
        url.contains("pub-eaa-providers") -> PUB_EAA_PROVIDERS
        url.contains("asitplus") -> ASIT_PLUS_PROVIDERS
        else -> null
    }
}