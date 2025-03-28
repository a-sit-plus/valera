package ui.composables.credentials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CredentialSelectionCardLayout(
    onClick: () -> Unit,
    modifier: Modifier,
    isSelected: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = mutableStateOf(Color.Unspecified)
    val borderStroke: MutableState<BorderStroke?> = mutableStateOf(null)

    if (isSelected) {
        color.value = MaterialTheme.colorScheme.primaryContainer
        borderStroke.value =
            BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.inversePrimary)
    } else {
        color.value = Color.Unspecified
        borderStroke.value = null
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(containerColor = color.value),
        border = borderStroke.value
    ) {
        Column(
            modifier = modifier.padding(8.dp).fillMaxWidth(),
        ) {
            content()
        }
    }
}