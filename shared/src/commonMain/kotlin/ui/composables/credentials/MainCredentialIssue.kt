package ui.composables.credentials

import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_credential_status_invalid
import at.asitplus.valera.resources.error_credential_timeliness_expired
import at.asitplus.valera.resources.error_credential_timeliness_not_yet_valid
import org.jetbrains.compose.resources.stringResource
import ui.composables.BigErrorText
import ui.composables.CredentialFreshnessSummaryModel

@Composable
fun MainCredentialIssue(credentialFreshnessSummaryModel: CredentialFreshnessSummaryModel) {
    when {
        credentialFreshnessSummaryModel.tokenStatus?.getOrNull()?.isInvalid == true -> {
            BigErrorText(stringResource(Res.string.error_credential_status_invalid))
        }

        !credentialFreshnessSummaryModel.timelinessIndicator.isTimely -> {
            if (credentialFreshnessSummaryModel.timelinessIndicator.isExpired) {
                BigErrorText(stringResource(Res.string.error_credential_timeliness_expired))
            } else if (credentialFreshnessSummaryModel.timelinessIndicator.isNotYetValid) {
                BigErrorText(stringResource(Res.string.error_credential_timeliness_not_yet_valid))
            }
        }
    }
}