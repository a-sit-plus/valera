package at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di

import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.IdentifierList
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListTokenPayload
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.PositiveDuration
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class StatusListTokenCachingDurationTest {
    private val resolvedAt = Clock.System.now()

    private fun payload(expirationTime: Instant? = null, timeToLive: Duration? = null) = StatusListTokenPayload(
        subject = UniformResourceIdentifier("https://example.test/status"),
        issuedAt = resolvedAt,
        expirationTime = expirationTime,
        timeToLive = timeToLive?.let(::PositiveDuration),
        revocationList = IdentifierList(emptyMap()),
    )

    @Test
    fun cachesATokenWithoutExpirationOrTtlForTheDefault() {
        assertEquals(Configuration.CACHE_TTL_TOKEN_STATUS, payload().cachingDuration(resolvedAt))
        assertEquals(Configuration.CACHE_TTL_TOKEN_STATUS, null.cachingDuration(resolvedAt))
    }

    @Test
    fun capsALongTtlAndADistantExpiration() {
        val payload = payload(expirationTime = resolvedAt + 365.days, timeToLive = 30.days)

        assertEquals(Configuration.CACHE_TTL_TOKEN_STATUS, payload.cachingDuration(resolvedAt))
    }

    @Test
    fun keepsAShorterTtl() {
        val payload = payload(expirationTime = resolvedAt + 30.days, timeToLive = 2.hours)

        assertEquals(2.hours, payload.cachingDuration(resolvedAt))
    }

    @Test
    fun keepsAnEarlierExpiration() {
        val payload = payload(expirationTime = resolvedAt + 3.hours, timeToLive = 12.hours)

        assertEquals(3.hours, payload.cachingDuration(resolvedAt))
    }
}
