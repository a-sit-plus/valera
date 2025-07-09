package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_combined
import at.asitplus.valera.resources.section_heading_request_combined
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.iso.VerifierViewModel

@Composable
fun CombinedRequest(
    layoutSpacingModifier: Modifier,
    vm: VerifierViewModel,
    selectedEngagementMethod: DeviceEngagementMethods,
    listSpacingModifier: Modifier
) {
    Column(modifier = layoutSpacingModifier) {
        Text(
            text = stringResource(Res.string.section_heading_request_combined),
            style = MaterialTheme.typography.titleMedium
        )
        RequestItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SwitchAccount,
                    contentDescription = null
                )
            },
            label = stringResource(Res.string.button_label_check_combined),
            onClick = { vm.navigateToCombinedSelectionView(selectedEngagementMethod) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
    }
}
