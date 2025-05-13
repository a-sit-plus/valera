package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.composables.CredentialFreshnessSummary
import ui.composables.buttons.DetailsButton


@Composable
fun ColumnScope.CredentialCardFooter(
    credentialFreshnessSummary: CredentialFreshnessSummary?,
    onOpenDetails: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    onOpenDetails?.let {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            Box {
                credentialFreshnessSummary?.let {
                    MainCredentialIssue(credentialFreshnessSummary)
                }
            }
            DetailsButton(
                onClick = onOpenDetails
            )
        }
    }
}