package ui.composables.buttons

import Resources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.composables.TextIconButton

@Composable
fun SaveButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
            )
        },
        text = {
            Text(Resources.BUTTON_LABEL_SAVE)
        },
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    )
}