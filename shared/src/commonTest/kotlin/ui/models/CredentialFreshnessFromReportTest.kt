package ui.models

import at.asitplus.wallet.app.common.validation.ValidationReportFixtures
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CheckOutcome.Blocked
import at.asitplus.wallet.lib.validation.CheckOutcome.Failed
import at.asitplus.wallet.lib.validation.CheckOutcome.Passed
import at.asitplus.wallet.lib.validation.StatusValidation
import at.asitplus.wallet.lib.validation.TimelinessException
import at.asitplus.wallet.lib.validation.TokenStatusException
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

internal class CredentialFreshnessFromReportTest : ValidationReportFixtures() {
    @Test
    fun freshCredentialWithoutStatus() {
        val freshness = credential().toCredentialFreshnessSummaryModel()

        assertTrue(freshness.isNotBad)
        assertNull(freshness.tokenStatus)
        assertFalse(freshness.needsRefresh)
    }

    @Test
    fun expiredCredentialNeedsRefresh() {
        val timeliness = Failed(TimelinessException("expired", now, notBefore = null, notAfter = now - 1.days))
        val freshness = credential(timeliness = timeliness).toCredentialFreshnessSummaryModel()

        assertTrue(freshness.timelinessIndicator.isExpired)
        assertFalse(freshness.timelinessIndicator.isNotYetValid)
        assertTrue(freshness.needsRefresh)
    }

    @Test
    fun notYetValidCredential() {
        val timeliness = Failed(TimelinessException("not yet valid", now, notBefore = now + 1.days, notAfter = null))
        val freshness = credential(timeliness = timeliness).toCredentialFreshnessSummaryModel()

        assertTrue(freshness.timelinessIndicator.isNotYetValid)
        assertFalse(freshness.needsRefresh)
    }

    @Test
    fun revokedCredentialNeedsRefresh() {
        val freshness = credential(
            status = status(statusListToken(), Failed(TokenStatusException(TokenStatus.Invalid)), TokenStatus.Invalid)
        ).toCredentialFreshnessSummaryModel()

        assertEquals(TokenStatus.Invalid, freshness.tokenStatus?.getOrNull())
        assertTrue(freshness.needsRefresh)
    }

    @Test
    fun suspendedCredentialDoesNotNeedRefresh() {
        val freshness = credential(
            status = status(
                statusListToken(),
                Failed(TokenStatusException(TokenStatus.Suspended)),
                TokenStatus.Suspended,
            )
        ).toCredentialFreshnessSummaryModel()

        assertEquals(TokenStatus.Suspended, freshness.tokenStatus?.getOrNull())
        assertFalse(freshness.needsRefresh)
    }

    @Test
    fun statusOfAnUntrustedSignerIsUnknown() {
        val untrusted = statusListToken(signerTrust = Failed(IllegalArgumentException("untrusted status list signer")))
        val freshness = credential(status = status(untrusted, Blocked(), observed = null))
            .toCredentialFreshnessSummaryModel()

        assertTrue(freshness.tokenStatus!!.isFailure)
        assertFalse(freshness.needsRefresh)
    }

    private fun status(token: ValidationReport, outcome: CheckOutcome, observed: TokenStatus?) = StatusValidation(
        claim = Passed,
        mechanisms = listOf(mechanism(token, outcome, observed)),
        agreement = if (outcome == Passed) Passed else Blocked(),
    )
}
