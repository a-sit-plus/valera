package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ScreenHeadingSubtitle(
    subTitle: String,
    modifier: Modifier = Modifier
) {
    AutoResizedText(
        text = subTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
    )
}
