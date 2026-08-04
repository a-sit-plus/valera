package ui.composables.credentials

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import ui.composables.CredentialCardActionMenu
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.ResolvedCredential


@Composable
fun ColumnScope.CredentialCardHeader(
    credential: ResolvedCredential,
    showLoadingSpinner: Boolean,
    showActionMenu: Boolean,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    credentialFreshnessSummaryModel: CredentialFreshnessSummaryUiModel?
) {
    PersonAttributeDetailCardHeading(
        icon = {
            PersonAttributeDetailCardHeadingIcon(credential.scheme.iconLabel())
        },
        title = {
            LabeledText(
                label = credential.entry.representation.uiLabel(),
                text = credential.scheme.uiLabel(),
            )
        },
    ) {
        if (showActionMenu) {
            CredentialCardActionMenu(
                showLoadingSpinner = showLoadingSpinner,
                onDelete = onDelete,
                onRefresh = onRefresh,
                credentialFreshnessSummaryModel = credentialFreshnessSummaryModel
            )
        }
    }
}
