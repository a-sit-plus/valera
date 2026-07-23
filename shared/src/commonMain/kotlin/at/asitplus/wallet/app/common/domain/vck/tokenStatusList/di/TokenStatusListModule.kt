package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.catching
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.data.primitives.CachingStatusListTokenResolver
import at.asitplus.wallet.app.common.data.primitives.SimpleBootstrappingBulkStore
import at.asitplus.wallet.app.common.data.primitives.SimpleCacheStoreWrapper
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListJwt
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import data.storage.DataStoreService
import data.storage.PersistentStatusListTokenStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock


fun tokenStatusListModule() = module {
    scope(named(SESSION_NAME)) {
        scoped<StatusListTokenResolver> {
            val httpService: HttpService by inject()
            val dataStoreService: DataStoreService by inject()
            val client = httpService.cachedResourceClient(dataStoreService, revalidate = true)
            CachingStatusListTokenResolver(
                store = SimpleCacheStoreWrapper(
                    store = SimpleBootstrappingBulkStore(
                        PersistentStatusListTokenStore(dataStoreService),
                    ),
                    clock = Clock.System,
                    getCachingDuration = { (_, value) ->
                        val payload = value.parsedPayload.getOrNull()
                        listOfNotNull(
                            payload?.expirationTime?.let { expiration ->
                                expiration - (value.resolvedAt ?: Clock.System.now())
                            },
                            payload?.timeToLive?.duration
                        ).minOrNull()?.also {
                            Napier.d("Entry specific caching duration is used: $it")
                        } ?: Configuration.CACHE_TTL_TOKEN_STATUS
                    },
                    onEntryFiltered = {
                        // Keep expired entries until a fresh token replaces them.
                    },
                ),
                statusListTokenResolver = {
                    val httpResponse = client.get(it.string) {
                        headers[HttpHeaders.Accept] = MediaTypes.Application.STATUSLIST_JWT
                    }
                    StatusListJwt(
                        catching { JwsCompactTyped<StatusListTokenPayload>(httpResponse.bodyAsText()) }.getOrThrow(),
                        resolvedAt = Clock.System.now(),
                    )
                }
            )
        }
    }
}
