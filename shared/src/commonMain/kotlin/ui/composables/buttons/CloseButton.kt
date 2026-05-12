package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_close
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
            )
        },
        text = {
            Text(
                stringResource(Res.string.button_label_close),
                textAlign = TextAlign.Center,
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}
