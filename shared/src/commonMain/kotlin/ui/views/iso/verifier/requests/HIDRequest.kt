package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_health_id
import at.asitplus.valera.resources.section_heading_request_hid
import at.asitplus.valera.resources.text_label_mandatory_attributes
import data.document.SelectableRequest
import data.document.SelectableRequestType
import org.jetbrains.compose.resources.stringResource

@Composable
fun HIDRequest(
    layoutSpacingModifier: Modifier,
    listSpacingModifier: Modifier,
    onRequestSelected: (SelectableRequest) -> Unit
) {
    Column(modifier = layoutSpacingModifier) {
        Text(
            text = stringResource(Res.string.section_heading_request_hid),
            style = MaterialTheme.typography.titleMedium
        )
        RequestItem(
            icon = { Icon(imageVector = Icons.Outlined.HealthAndSafety, contentDescription = null) },
            label = stringResource(Res.string.button_label_check_health_id),
            subLabel = stringResource(Res.string.text_label_mandatory_attributes),
            onClick = { onRequestSelected(SelectableRequest(SelectableRequestType.HIID)) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
    }
}
