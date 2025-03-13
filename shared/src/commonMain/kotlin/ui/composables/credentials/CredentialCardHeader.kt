package ui.composables.credentials

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.composables.CredentialCardActionMenu
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon


@Composable
fun ColumnScope.CredentialCardHeader(
    credential: SubjectCredentialStore.StoreEntry,
    onDelete: () -> Unit,
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
    ) {
        CredentialCardActionMenu(
            onDelete = onDelete
        )
    }
}