package ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class TextIconButtonDefaults {
    companion object {
        val contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp, start = 16.dp, end = 24.dp)
        val gapSize = 8.dp
    }
}

@Composable
fun TextIconButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit = {},
    contentPadding: PaddingValues = TextIconButtonDefaults.contentPadding,
    spacer: @Composable () -> Unit = {
        Spacer(modifier = Modifier.width(TextIconButtonDefaults.gapSize))
    },
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()

            spacer()

            text()
        }
    }
}

@Composable
fun OutlinedTextIconButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit = {},
    contentPadding: PaddingValues = TextIconButtonDefaults.contentPadding,
    spacer: @Composable () -> Unit = {
        Spacer(modifier = Modifier.width(TextIconButtonDefaults.gapSize))
    },
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()

            spacer()

            text()
        }
    }
}