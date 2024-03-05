package ui.composables.buttons

import Resources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.composables.TextIconButton

@Composable
fun AcceptButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
            )
        },
        text = {
            Text(Resources.BUTTON_LABEL_ACCEPT)
        },
        onClick = onClick,
        modifier = modifier,
    )
}

