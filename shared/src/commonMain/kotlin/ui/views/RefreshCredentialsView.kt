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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_clear_and_close
import at.asitplus.valera.resources.button_dismiss
import at.asitplus.valera.resources.button_refresh
import at.asitplus.valera.resources.button_remove
import at.asitplus.valera.resources.error_unknown
import at.asitplus.valera.resources.refresh_center_empty_state
import at.asitplus.valera.resources.refresh_center_title
import at.asitplus.valera.resources.refresh_status_failed
import at.asitplus.valera.resources.refresh_status_in_progress
import at.asitplus.valera.resources.refresh_status_pending
import at.asitplus.valera.resources.refresh_status_succeeded
import at.asitplus.wallet.app.common.RefreshItem
import at.asitplus.wallet.app.common.RefreshStatus
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshCredentialsView(
    items: List<RefreshItem>,
    onRefreshItem: (RefreshItem) -> Unit,
    onRemoveItem: (RefreshItem) -> Unit,
    onDone: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.refresh_center_title), fontWeight = FontWeight.Bold)
            TextButton(onClick = onDone) {
                Text(stringResource(Res.string.button_clear_and_close))
            }
        }

        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.refresh_center_empty_state))
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> "${item.entry.getDcApiId()}-$index" }
                ) { _, item ->
                    RefreshItemRow(
                        item = item,
                        onRefresh = { onRefreshItem(item) },
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
            val statusColor = when (item.status) {
                RefreshStatus.Pending -> LocalContentColor.current
                RefreshStatus.InProgress -> Color(0xFF1565C0)
                RefreshStatus.Succeeded -> Color(0xFF2E7D32)
                RefreshStatus.Failed -> Color(0xFFC62828)
            }

            val statusText = when (item.status) {
                RefreshStatus.Pending -> stringResource(Res.string.refresh_status_pending)
                RefreshStatus.InProgress -> stringResource(Res.string.refresh_status_in_progress)
                RefreshStatus.Succeeded -> stringResource(Res.string.refresh_status_succeeded)
                RefreshStatus.Failed -> {
                    val errorMsg = item.error ?: stringResource(Res.string.error_unknown)
                    stringResource(Res.string.refresh_status_failed, errorMsg)
                }
            }
            Text(statusText, color = statusColor)
        }

        when (item.status) {
            RefreshStatus.InProgress -> {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            RefreshStatus.Succeeded -> {
                TextButton(onClick = onRemove) { Text(stringResource(Res.string.button_dismiss)) }
            }
            else -> {
                Row {
                    TextButton(onClick = onRemove) { Text(stringResource(Res.string.button_remove), color = Color.Gray) }
                    Button(onClick = onRefresh) { Text(stringResource(Res.string.button_refresh)) }
                }
            }
        }
    }
}