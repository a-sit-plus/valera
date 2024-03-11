package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TextIcon(
    text: String,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = TextIconDefaults.color(),
    contentColor: Color = contentColorFor(color),
) {
    TextIcon(
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
fun TextIcon(
    text: String,
    fontWeight: FontWeight = FontWeight.Bold,
    colors: IconButtonColors,
) {
    IconButton(
        onClick = {},
        enabled = false,
        colors = colors,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().wrapContentSize(
                align = Alignment.Center
            ),
        ) {
            Text(
                text = text,
                fontWeight = fontWeight,
            )
        }
    }
}

class TextIconDefaults {
    companion object {
        @Composable
        fun color(
            color: Color = MaterialTheme.colorScheme.primary,
        ): Color = color
    }
}