package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RequestSection(
    title: String,
    layoutSpacingModifier: Modifier,
    listSpacingModifier: Modifier,
    requestItems: List<RequestItemData>,
    extraContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = layoutSpacingModifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        requestItems.forEach { item ->
            RequestItem(
                requestItemData = item,
                modifier = listSpacingModifier
            )
        }
        extraContent?.invoke()
    }
}
