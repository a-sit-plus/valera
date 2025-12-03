package ui.views.iso.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.section_heading_request_engagement_method
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import org.jetbrains.compose.resources.stringResource

@Composable
fun RequestEngagementMethodView(
    selectedEngagementMethod: DeviceEngagementMethods,
    onSelect: (DeviceEngagementMethods) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.section_heading_request_engagement_method),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        DeviceEngagementMethods.entries.forEach { engagementMethod ->
            SingleChoiceButton(
                current = engagementMethod.friendlyName,
                selectedOption = selectedEngagementMethod.friendlyName,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                icon = { Icon(engagementMethod.icon, null) }
            ) { onSelect(engagementMethod) }
        }
    }
}
