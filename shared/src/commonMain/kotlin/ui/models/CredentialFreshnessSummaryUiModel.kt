package ui.models

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.validation.StoredCredentialValidator
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CredentialChecks
import at.asitplus.wallet.lib.validation.TimelinessException
import at.asitplus.wallet.lib.validation.ValidationDecision
import at.asitplus.wallet.lib.validation.ValidationReport
import at.asitplus.wallet.lib.validation.cause

data class CredentialFreshnessSummaryUiModel(
    val timelinessIndicator: TimelinessIndicatorUiModel,
    val tokenStatus: KmmResult<TokenStatus>?,
) {
    val isNotBad = timelinessIndicator.isTimely && (tokenStatus?.getOrNull()?.isInvalid != true)

    /** Whether a credential is to be refreshed: it is expired, or its status is verified to be invalid. */
    val needsRefresh: Boolean
        get() = timelinessIndicator.isExpired || tokenStatus?.getOrNull()?.isInvalid == true
}

/**
 * The freshness of a credential from its report, see [StoredCredentialValidator]: whether it is expired or not yet
 * valid, and its status, if it has one, which is known only if a status list token of a trusted signer yields it.
 */
fun ValidationReport.toCredentialFreshnessSummaryModel(): CredentialFreshnessSummaryUiModel {
    val checks = requireNotNull(checks as? CredentialChecks) { "Freshness is only defined for credentials" }
    val timeliness = checks.timeliness.outcome.cause() as? TimelinessException
    val status = checks.status
    return CredentialFreshnessSummaryUiModel(
        timelinessIndicator = object : TimelinessIndicatorUiModel {
            override val isExpired = timeliness?.notAfter?.let { evaluatedAt > it } == true
            override val isNotYetValid = timeliness?.notBefore?.let { evaluatedAt < it } == true
        },
        tokenStatus = if (status.claim == CheckOutcome.NotApplicable) null else {
            status.mechanisms
                .firstOrNull { it.token.decision == ValidationDecision.ACCEPTED && it.observed != null }
                ?.observed?.let { KmmResult.success(it) }
                ?: KmmResult.failure(
                    status.mechanisms.firstNotNullOfOrNull { it.outcome.cause() }
                        ?: status.claim.cause()
                        ?: IllegalStateException("The status could not be checked")
                )
        },
    )
}
