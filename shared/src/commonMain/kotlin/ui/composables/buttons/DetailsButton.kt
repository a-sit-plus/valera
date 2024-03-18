@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import composewalletapp.shared.generated.resources.button_label_details
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DetailsButton(
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(Res.string.button_label_details))
        },
        onClick = onClick,
        colors = colors,
        modifier = modifier,
    )
}