package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.data.primitives.CacheStoreEntry
import at.asitplus.wallet.app.common.data.primitives.CachingStatusListTokenResolver
import at.asitplus.wallet.app.common.data.primitives.SimpleBootstrappingBulkStore
import at.asitplus.wallet.app.common.data.primitives.SimpleCacheStoreWrapper
import at.asitplus.wallet.app.common.data.primitives.SimpleMutableMapStore
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.StatusListTokenResolver
import at.asitplus.wallet.lib.data.StatusListToken
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.MediaTypes
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlin.time.Clock
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds


fun tokenStatusListModule() = module {
    single<StatusListTokenResolver> {
        val httpService: HttpService by inject()
        CachingStatusListTokenResolver(
            store = SimpleCacheStoreWrapper(
                store = SimpleBootstrappingBulkStore(
                    SimpleMutableMapStore<UniformResourceIdentifier, CacheStoreEntry<StatusListToken>>(),
                ),
                clock = Clock.System,
                getCachingDuration = { (key, value) ->
                    listOfNotNull(
                        value.payload.expirationTime?.let { it - Clock.System.now() },
                        value.payload.timeToLive?.duration
                    ).minOrNull()?.also {
                        Napier.d("Entry specific caching duration is used: $it")
                    } ?: 300.seconds
                },
                onEntryFiltered = {
                    // Let's not actually remove anything for now, token status list urls do not change between fetches anyway
                },
            ),
            statusListTokenResolver = {
                val httpResponse = httpService.buildHttpClient().get(it.string) {
                    headers[HttpHeaders.Accept] = MediaTypes.Application.STATUSLIST_JWT
                }
                StatusListToken.StatusListJwt(
                    JwsSigned.deserialize<StatusListTokenPayload>(
                        StatusListTokenPayload.serializer(),
                        httpResponse.bodyAsText()
                    ).getOrThrow(),
                    resolvedAt = Clock.System.now(),
                )
            }
        )
    }
}