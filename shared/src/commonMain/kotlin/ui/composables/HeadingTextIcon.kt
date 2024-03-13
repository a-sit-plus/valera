package ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HeadingTextIcon(
    text: String,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = TextIconDefaults.color(),
    contentColor: Color = contentColorFor(color),
) {
    HeadingTextIcon(
        text = text,
        fontWeight = fontWeight,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = color,
            disabledContentColor = contentColor,
        ),
    )
}

@Composable
fun HeadingTextIcon(
    text: String,
    fontWeight: FontWeight = FontWeight.Bold,
    colors: IconButtonColors,
) {
    HeadingTextIconContainer {
        TextIcon(
            text = text,
            fontWeight = fontWeight,
            colors = colors,
        )
    }
}

@Composable
fun HeadingTextIconContainer(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(width = 40.dp, height = 40.dp)
    ) {
        content()
    }
}