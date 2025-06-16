package ui.models

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.agent.validation.CredentialTimelinessValidationSummary
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult

data class CredentialFreshnessSummaryUiModel(
    val timelinessIndicator: TimelinessIndicatorUiModel,
    val tokenStatus: KmmResult<TokenStatus>?,
) {
    constructor(
        timelinessValidationSummary: CredentialTimelinessValidationSummary,
        tokenStatus: KmmResult<TokenStatus>?,
    ) : this(
        timelinessIndicator = object : TimelinessIndicatorUiModel {
            override val isExpired = when (val it = timelinessValidationSummary) {
                is CredentialTimelinessValidationSummary.Mdoc -> it.details.msoTimelinessValidationSummary?.mdocExpiredError != null
                is CredentialTimelinessValidationSummary.SdJwt -> it.details.jwsExpiredError != null
                is CredentialTimelinessValidationSummary.VcJws -> it.details.jwsExpiredError != null || it.details.credentialExpiredError != null
            }

            override val isNotYetValid = when (val it = timelinessValidationSummary) {
                is CredentialTimelinessValidationSummary.Mdoc -> it.details.msoTimelinessValidationSummary?.mdocNotYetValidError != null
                is CredentialTimelinessValidationSummary.SdJwt -> it.details.jwsNotYetValidError != null
                is CredentialTimelinessValidationSummary.VcJws -> it.details.jwsNotYetValidError != null || it.details.credentialNotYetValidError != null
            }
        },
        tokenStatus = tokenStatus,
    )

    val isNotBad = timelinessIndicator.isTimely && (tokenStatus?.getOrNull()?.isInvalid != true)
}

fun CredentialFreshnessSummary.toCredentialFreshnessSummaryModel(): CredentialFreshnessSummaryUiModel {
    return CredentialFreshnessSummaryUiModel(
        timelinessValidationSummary = timelinessValidationSummary,
        tokenStatus = when (val it = tokenStatusValidationResult) {
            is TokenStatusValidationResult.Invalid -> KmmResult.success(it.tokenStatus)
            is TokenStatusValidationResult.Rejected -> KmmResult.failure(it.throwable)
            is TokenStatusValidationResult.Valid -> it.tokenStatus?.let {
                KmmResult.success(it)
            }
        }
    )
}
