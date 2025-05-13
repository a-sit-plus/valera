package ui.composables

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.validation.CredentialTimelinessValidationSummary
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus

data class CredentialFreshnessSummary(
    val timelinessValidationSummary: CredentialTimelinessValidationSummary,
    val tokenStatus: KmmResult<TokenStatus>?,
) {
    val isGood = timelinessValidationSummary.isSuccess && (tokenStatus?.getOrNull()?.isInvalid != true)
}