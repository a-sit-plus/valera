package ui.views.iso.verifier.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_custom
import at.asitplus.valera.resources.section_heading_request_custom
import at.asitplus.wallet.app.common.iso.transfer.DeviceEngagementMethods
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.iso.VerifierViewModel

@Composable
fun CustomRequest(
    layoutSpacingModifier: Modifier,
    vm: VerifierViewModel,
    selectedEngagementMethod: DeviceEngagementMethods,
    listSpacingModifier: Modifier
) {
    Column(modifier = layoutSpacingModifier) {
        Text(
            text = stringResource(Res.string.section_heading_request_custom),
            style = MaterialTheme.typography.titleMedium
        )
        RequestItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null
                )
            },
            label = stringResource(Res.string.button_label_check_custom),
            onClick = { vm.navigateToCustomSelectionView(selectedEngagementMethod) },
            modifier = listSpacingModifier.fillMaxWidth()
        )
    }
}
