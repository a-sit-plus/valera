package ui.composables.credentials

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon


@Composable
fun ColumnScope.CredentialSelectionCardHeader(
    credentialFreshnessValidationState: CredentialFreshnessValidationStateUiModel,
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    PersonAttributeDetailCardHeading(
        icon = {
            PersonAttributeDetailCardHeadingIcon(credential.scheme.iconLabel())
        },
        title = {
            LabeledText(
                label = credential.representation.uiLabel(),
                text = credential.scheme.uiLabel(),
            )
        },
        actionButtons = {
            when(val it = credentialFreshnessValidationState) {
                CredentialFreshnessValidationStateUiModel.Loading -> CircularProgressIndicator()
                is CredentialFreshnessValidationStateUiModel.Done -> MainCredentialIssue(it.credentialFreshnessSummary)
            }
        }
    )
}