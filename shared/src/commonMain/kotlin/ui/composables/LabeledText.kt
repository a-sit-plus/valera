package ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink

@Composable
fun LabeledText(
    text: String,
    label: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1
) = LabeledContent(
    label = label,
    modifier = modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun LabeledHyperlinkText(
    text: String,
    url: String,
    label: String,
    modifier: Modifier = Modifier,
) = LabeledContent(
    label = label,
    modifier = modifier,
) {
    Text(
        text = buildAnnotatedString {
            withLink(
                LinkAnnotation.Url(
                    url,
                    TextLinkStyles(style = SpanStyle(color = Color.Blue)),
                )
            ) {
                append(text)
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}