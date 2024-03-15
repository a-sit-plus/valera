@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import composewalletapp.shared.generated.resources.BUTTON_LABEL_CANCEL
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
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
            Text(stringResource(Res.string.BUTTON_LABEL_CANCEL))
        },
        onClick = onClick,
        modifier = modifier,
    )
}