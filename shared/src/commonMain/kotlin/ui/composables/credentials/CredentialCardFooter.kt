package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon
import ui.composables.buttons.DetailsButton
import ui.composables.buttons.SingleCredentialCardDeleteButton


@Composable
fun ColumnScope.CredentialCardFooter(
    onOpenDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    onOpenDetails?.let {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = modifier,
        ) {
            DetailsButton(
                onClick = onOpenDetails
            )
        }
    }
}