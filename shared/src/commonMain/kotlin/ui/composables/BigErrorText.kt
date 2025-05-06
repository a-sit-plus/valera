package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BigErrorText(
    text: String,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.ExtraBold,
    )
}