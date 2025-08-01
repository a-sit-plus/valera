package ui.views.iso.verifier.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RequestItem(
    requestItemData: RequestItemData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = requestItemData.onClick)
            .padding(top = 4.dp, end = 16.dp, bottom = 4.dp, start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        requestItemData.icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = requestItemData.label,
                style = MaterialTheme.typography.bodyLarge
            )
            requestItemData.subLabel?.let {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

data class RequestItemData(
    val icon: @Composable () -> Unit = {},
    val label: String,
    val subLabel: String? = null,
    val onClick: () -> Unit
)
