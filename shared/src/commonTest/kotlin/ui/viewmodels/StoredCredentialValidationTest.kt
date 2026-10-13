package ui.viewmodels

import at.asitplus.wallet.app.common.validation.ValidationPolicies
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CheckOutcome.Blocked
import at.asitplus.wallet.lib.validation.CheckOutcome.Failed
import at.asitplus.wallet.lib.validation.CheckOutcome.NotApplicable
import at.asitplus.wallet.lib.validation.CheckOutcome.Passed
import at.asitplus.wallet.lib.validation.CredentialChecks
import at.asitplus.wallet.lib.validation.StatusValidation
import at.asitplus.wallet.lib.validation.TimelinessValidation
import at.asitplus.wallet.lib.validation.TrustValidation
import at.asitplus.wallet.lib.validation.ValidationReport
import ui.composables.TrustState
import ui.viewmodels.CredentialDetailsViewModel.StoredCredentialValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class StoredCredentialValidationTest {
    @Test
    fun issuerTrustStateFollowsTheReport() {
        assertEquals(TrustState.TRUSTED, validation(Passed).issuerTrustState)
        assertEquals(TrustState.UNTRUSTED, validation(Failed(IllegalArgumentException("untrusted"))).issuerTrustState)
        assertEquals(TrustState.UNKNOWN, validation(Blocked()).issuerTrustState)
        assertEquals(TrustState.UNKNOWN, StoredCredentialValidation(null).issuerTrustState)
    }

    private fun validation(issuerTrust: CheckOutcome) = StoredCredentialValidation(
        ValidationReport.credential(
            checks = CredentialChecks(
                parsing = Passed,
                issuerSignature = Passed,
                issuerTrust = TrustValidation(issuerTrust, source = "pid-providers.json"),
                semantics = Passed,
                disclosedItems = emptyList(),
                holderBinding = NotApplicable,
                timeliness = TimelinessValidation(Passed, details = null),
                status = StatusValidation(claim = NotApplicable, agreement = NotApplicable),
            ),
            policy = ValidationPolicies.strict,
            evaluatedAt = Instant.parse("2026-10-13T12:00:00Z"),
        ).toUi()
    )
}
