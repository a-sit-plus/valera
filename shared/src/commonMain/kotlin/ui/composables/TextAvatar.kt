package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TextAvatar(
    text: String?,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = onClick != null,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick ?: {},
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
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
                    modifier = Modifier.alignByBaseline(),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}