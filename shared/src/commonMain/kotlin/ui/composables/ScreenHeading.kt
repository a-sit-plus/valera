package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScreenHeading(title: String, modifier: Modifier = Modifier) {
    AutoResizedText(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium
    )
}
