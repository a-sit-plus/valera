package at.asitplus.wallet.app.common

import kotlin.test.Test
import kotlin.test.assertEquals
import ui.composables.TrustState

class RelyingPartyTrustAggregationTest {

    @Test
    fun emptySignerListIsUnknown() {
        assertEquals(RelyingPartyTrustSummary.UNKNOWN, aggregateRelyingPartyTrust(emptyList()))
    }

    @Test
    fun identicalSignerStatesKeepTheirState() {
        assertEquals(
            RelyingPartyTrustSummary.TRUSTED,
            aggregateRelyingPartyTrust(listOf(TrustState.TRUSTED, TrustState.TRUSTED)),
        )
        assertEquals(
            RelyingPartyTrustSummary.UNTRUSTED,
            aggregateRelyingPartyTrust(listOf(TrustState.UNTRUSTED, TrustState.UNTRUSTED)),
        )
        assertEquals(
            RelyingPartyTrustSummary.UNKNOWN,
            aggregateRelyingPartyTrust(listOf(TrustState.UNKNOWN, TrustState.UNKNOWN)),
        )
    }

    @Test
    fun differingStatesWithTrustedSignerAreMixedWithTrusted() {
        listOf(
            listOf(TrustState.TRUSTED, TrustState.UNTRUSTED),
            listOf(TrustState.UNTRUSTED, TrustState.TRUSTED),
            listOf(TrustState.TRUSTED, TrustState.UNKNOWN),
            listOf(TrustState.UNKNOWN, TrustState.TRUSTED),
        ).forEach { states ->
            assertEquals(
                RelyingPartyTrustSummary.MIXED_WITH_TRUSTED,
                aggregateRelyingPartyTrust(states),
            )
        }
    }

    @Test
    fun differingStatesWithoutTrustedSignerAreMixedWithoutTrusted() {
        assertEquals(
            RelyingPartyTrustSummary.MIXED_WITHOUT_TRUSTED,
            aggregateRelyingPartyTrust(listOf(TrustState.UNTRUSTED, TrustState.UNKNOWN)),
        )
        assertEquals(
            RelyingPartyTrustSummary.MIXED_WITHOUT_TRUSTED,
            aggregateRelyingPartyTrust(listOf(TrustState.UNKNOWN, TrustState.UNTRUSTED)),
        )
    }
}
