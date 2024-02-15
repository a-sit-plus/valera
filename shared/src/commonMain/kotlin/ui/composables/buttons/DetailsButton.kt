package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.composables.TextIconButton

@Composable
fun DetailsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
            )
        },
        text = {
            Text(Resources.BUTTON_LABEL_DETAILS)
        },
        onClick = onClick,
        modifier = modifier,
    )
}