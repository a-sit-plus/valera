package ui.composables.credentials

import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_credential_status_invalid
import at.asitplus.valera.resources.error_credential_timeliness_expired
import at.asitplus.valera.resources.error_credential_timeliness_not_yet_valid
import at.asitplus.wallet.lib.agent.validation.CredentialTimelinessValidationSummary
import org.jetbrains.compose.resources.stringResource
import ui.composables.BigErrorText
import ui.composables.CredentialFreshnessSummary

@Composable
fun MainCredentialIssue(credentialFreshnessSummary: CredentialFreshnessSummary) {
    when {
        credentialFreshnessSummary.tokenStatus?.getOrNull()?.isInvalid == true -> {
            BigErrorText(stringResource(Res.string.error_credential_status_invalid))
        }

        !credentialFreshnessSummary.timelinessValidationSummary.isSuccess -> {
            val isExpired = when (val it = credentialFreshnessSummary.timelinessValidationSummary) {
                is CredentialTimelinessValidationSummary.Mdoc -> it.details.msoTimelinessValidationSummary?.mdocExpiredError != null
                is CredentialTimelinessValidationSummary.SdJwt -> it.details.jwsExpiredError != null
                is CredentialTimelinessValidationSummary.VcJws -> it.details.jwsExpiredError != null || it.details.credentialExpiredError != null
            }
            val isNotYetValid = when (val it = credentialFreshnessSummary.timelinessValidationSummary) {
                is CredentialTimelinessValidationSummary.Mdoc -> it.details.msoTimelinessValidationSummary?.mdocNotYetValidError != null
                is CredentialTimelinessValidationSummary.SdJwt -> it.details.jwsNotYetValidError != null
                is CredentialTimelinessValidationSummary.VcJws -> it.details.jwsNotYetValidError != null || it.details.credentialNotYetValidError != null
            }
            if (isExpired) {
                BigErrorText(stringResource(Res.string.error_credential_timeliness_expired))
            } else if (isNotYetValid) {
                BigErrorText(stringResource(Res.string.error_credential_timeliness_not_yet_valid))
            }
        }
    }
}