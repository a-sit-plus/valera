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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CredentialSelectionCardLayout(
    isError: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    isSelected: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = when {
        isError -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        isSelected -> CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        else -> CardDefaults.elevatedCardColors()
    }
    val border = when {
        !isSelected -> null
        isError -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        else -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    }

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = colors,
        border = border,
    ) {
        Column(
            modifier = modifier.padding(8.dp).fillMaxWidth(),
        ) {
            content()
        }
    }
}
