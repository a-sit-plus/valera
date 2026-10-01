@file:OptIn(ExperimentalResourceApi::class)

package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ContinueButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: StringResource = Res.string.button_label_continue,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
            )
        },
        text = {
            Text(stringResource(label))
        },
        enabled = onClick != null,
        onClick = onClick ?: {},
        modifier = modifier,
    )
}
