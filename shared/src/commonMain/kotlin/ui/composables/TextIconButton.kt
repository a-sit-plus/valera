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
import ui.defaults.ButtonDefaultOverrides

@Composable
fun TextIconButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit = {},
    contentPadding: PaddingValues = ButtonDefaultOverrides.ContentPadding.TextIconButton,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()

            Spacer(modifier = Modifier.width(8.dp))

            text()
        }
    }
}
@Composable
fun OutlinedTextIconButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit = {},
    contentPadding: PaddingValues = ButtonDefaultOverrides.ContentPadding.TextIconButton,
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

            Spacer(modifier = Modifier.width(8.dp))

            text()
        }
    }
}