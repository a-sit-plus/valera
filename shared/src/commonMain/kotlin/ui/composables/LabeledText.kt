package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun LabeledText(
    text: String,
    label: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
) = LabeledContent(
    label = label,
    modifier = modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
