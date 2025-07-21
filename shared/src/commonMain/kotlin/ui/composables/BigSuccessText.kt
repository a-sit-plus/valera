package ui.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import ui.theme.LocalExtendedColors

@Composable
fun BigSuccessText(
    text: String,
) {
    val extendedColors = LocalExtendedColors.current
    Text(
        text = text,
        color = extendedColors.success,
        fontWeight = FontWeight.ExtraBold,
    )
}
