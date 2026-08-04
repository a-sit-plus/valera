package at.asitplus.wallet.app.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TrustListFreshnessTest {
    @Test
    fun dropsEntriesOlderThanTtl() {
        val now = Instant.fromEpochMilliseconds(10.days.inWholeMilliseconds)
        val fresh = now - 6.days
        val stale = now - 8.days

        val result = mapOf(
            "fresh" to ("F" to fresh),
            "stale" to ("S" to stale),
        ).filterFresh(now, 7.days)

        assertEquals(mapOf("fresh" to "F"), result)
    }

    @Test
    fun schedulesRefreshForEarliestExpiry() {
        val now = Instant.fromEpochMilliseconds(10.days.inWholeMilliseconds)

        assertEquals(
            12.hours,
            listOf(now - 6.days - 12.hours, now - 1.days).nextRefreshIn(now, 7.days),
        )
        assertEquals(
            kotlin.time.Duration.ZERO,
            listOf(now - 8.days).nextRefreshIn(now, 7.days),
        )
    }
}
