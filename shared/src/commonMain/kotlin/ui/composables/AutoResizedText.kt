package ui.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minFontSize: TextUnit = 12.sp,
    color: Color = Color.Unspecified
) {
    var textStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        style = textStyle,
        fontWeight = fontWeight,
        maxLines = maxLines,
        color = color,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && textStyle.fontSize > minFontSize) {
                textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        },
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        }
    )
}
