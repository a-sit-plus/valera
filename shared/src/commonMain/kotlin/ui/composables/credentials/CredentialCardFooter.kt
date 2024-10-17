package ui.composables.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.composables.buttons.DetailsButton


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