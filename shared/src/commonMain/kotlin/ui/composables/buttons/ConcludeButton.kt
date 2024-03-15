package ui.composables.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import composewalletapp.shared.generated.resources.BUTTON_LABEL_CONCLUDE
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIconButton

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ConcludeButton(
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
            Text(
                stringResource(Res.string.BUTTON_LABEL_CONCLUDE),
                textAlign = TextAlign.Center,
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}