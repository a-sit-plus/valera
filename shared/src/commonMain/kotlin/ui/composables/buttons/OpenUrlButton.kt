package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_open_url
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@Composable
fun OpenUrlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
            )
        },
        text = {
            Text(
                stringResource(Res.string.button_label_open_url),
                textAlign = TextAlign.Center,
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}