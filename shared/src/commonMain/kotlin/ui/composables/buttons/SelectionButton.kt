@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_select
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.OutlinedTextIconButton

@Composable
fun SelectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextIconButton(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(Res.string.button_label_select))
        },
        onClick = onClick,
        modifier = modifier,
    )
}