@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import composewalletapp.shared.generated.resources.BUTTON_LABEL_ACCEPT
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
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
            Text(stringResource(Res.string.BUTTON_LABEL_ACCEPT))
        },
        onClick = onClick,
        modifier = modifier,
    )
}

