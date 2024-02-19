package ui.composables.buttons

import Resources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import ui.composables.TextIconButton

@Composable
fun LoadDataButton(
    onClick: () -> Unit,
) {
    TextIconButton(
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
            )
        },
        text = {
            Text(
                Resources.BUTTOM_LABEL_LOAD_DATA,
                textAlign = TextAlign.Center,
            )
        },
        onClick = onClick,
    )
}