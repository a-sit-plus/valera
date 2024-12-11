package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_cancel
import org.jetbrains.compose.resources.stringResource
import ui.composables.OutlinedTextIconButton

@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(Res.string.button_label_cancel))
        },
        onClick = onClick,
        modifier = modifier,
    )
}