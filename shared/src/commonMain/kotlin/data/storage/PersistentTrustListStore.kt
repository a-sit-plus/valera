package data.storage

import at.asitplus.etsi.ListOfTrustedEntities
import at.asitplus.etsi.TrustListPayload
import at.asitplus.signum.indispensable.josef.JwsCompact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CachedTrustList(
    val loTe: ListOfTrustedEntities,
    val lastFetched: Long
)

class PersistentTrustListStore(
    private val dataStoreService: DataStoreService,
) {

    suspend fun persistTrustList(url: String, rawJwsText: String, timestampMillis: Long) {
        val key = TrustListStorageKeys.mapUrlToKey(url) ?: return
        dataStoreService.setPreference(rawJwsText, key)
        dataStoreService.setPreference(timestampMillis.toString(), "${key}_last_success")
    }

    fun observeTrustContainer(): Flow<Map<String, CachedTrustList>> {
        val keys = listOf(
            TrustListStorageKeys.PID_PROVIDERS,
            TrustListStorageKeys.WALLET_PROVIDERS,
            TrustListStorageKeys.WRPAC_PROVIDERS,
            TrustListStorageKeys.MDL_PROVIDERS,
            TrustListStorageKeys.PUB_EAA_PROVIDERS,
            TrustListStorageKeys.ASIT_PLUS_PROVIDERS
        )

        val flows = keys.map { key ->
            combine(
                dataStoreService.getPreference(key),
                dataStoreService.getPreference("${key}_last_success")
            ) { rawJws, timestampStr ->
                val lote = parseLoTE(rawJws)
                val timestamp = timestampStr?.toLongOrNull() ?: 0L

                if (lote != null) {
                    key to CachedTrustList(lote, timestamp)
                } else {
                    key to null
                }
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