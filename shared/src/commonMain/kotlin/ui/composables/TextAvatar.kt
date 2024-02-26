package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
fun TextAvatar(
    text: String?,
    fontWeight: FontWeight = FontWeight.Bold,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = onClick != null,
    colors: IconButtonColors = TextAvatarDefaults.backgroundColor(),
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick ?: {},
        colors = colors,
        enabled = enabled,
        modifier = modifier,
    ) {
        if(text != null) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    fontWeight = fontWeight,
                )
            }
        }
    }
}

class TextAvatarDefaults {
    companion object {
        @Composable
        fun backgroundColor(
            containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor: Color = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
            disabledContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor: Color = contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
        ) = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
    }
}