package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.RefreshItem
import at.asitplus.wallet.app.common.RefreshStatus
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun RefreshCredentialsView(
    items: List<RefreshItem>,
    onRefreshItem: (RefreshItem) -> Unit, // Changed from onRefreshSelected
    onRemoveItem: (RefreshItem) -> Unit,
    onDone: () -> Unit // New callback for the back button
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Refresh Center", fontWeight = FontWeight.Bold)
            TextButton(onClick = onDone) {
                Text("Clear & Close")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No credentials need refreshing.")
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "${item.entry.getDcApiId()}-$index" }
                ) { _, item ->
                    RefreshItemRow(
                        item = item,
                        onRefresh = { onRefreshItem(item) }, // Individual refresh
                        onRemove = { onRemoveItem(item) }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun RefreshItemRow(
    item: RefreshItem,
    onRefresh: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(item.entry.scheme.uiLabel(), fontWeight = FontWeight.SemiBold)
            val (statusText, statusColor) = when (item.status) {
                RefreshStatus.Pending -> "Needs Update" to LocalContentColor.current
                RefreshStatus.InProgress -> "Refreshing…" to Color(0xFF1565C0)
                RefreshStatus.Succeeded -> "Successfully Updated" to Color(0xFF2E7D32)
                RefreshStatus.Failed -> "Failed: ${item.error ?: "Unknown error"}" to Color(0xFFC62828)
            }
            Text(statusText, color = statusColor)
        }

        when (item.status) {
            RefreshStatus.InProgress -> {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            RefreshStatus.Succeeded -> {
                TextButton(onClick = onRemove) { Text("Dismiss") }
            }
            else -> {
                Row {
                    TextButton(onClick = onRemove) { Text("Remove", color = Color.Gray) }
                    Button(onClick = onRefresh) { Text("Refresh") }
                }
            }
        }
    }
}