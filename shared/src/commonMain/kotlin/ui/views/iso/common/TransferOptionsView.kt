package ui.views.iso.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.section_heading_transfer_options
import at.asitplus.valera.resources.switch_label_blel2cap_enabled
import at.asitplus.valera.resources.switch_label_use_ble_central_client_mode
import at.asitplus.valera.resources.switch_label_use_ble_peripheral_server_mode
import at.asitplus.valera.resources.switch_label_use_negotiated_handover
import at.asitplus.valera.resources.switch_label_use_nfc_data_transfer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.viewmodels.iso.common.TransferOptionsViewModel

@Composable
fun TransferOptionsView(
    layoutSpacingModifier: Modifier,
    koinScope: Scope,
    transferOptionsViewModel: TransferOptionsViewModel = koinViewModel(scope = koinScope)
) {
    Column(
        modifier = layoutSpacingModifier
    ) {
        val listSpacingModifier = Modifier
        Text(
            text = stringResource(Res.string.section_heading_transfer_options),
            style = MaterialTheme.typography.titleMedium,
        )
        SettingSwitch(
            label = stringResource(Res.string.switch_label_use_negotiated_handover),
            modifier = listSpacingModifier.fillMaxWidth(),
            isChecked = transferOptionsViewModel.presentmentUseNegotiatedHandover.collectAsState().value,
            onCheckedChange = { transferOptionsViewModel.setPresentmentUseNegotiatedHandover(it) }
        )
        SettingSwitch(
            label = stringResource(Res.string.switch_label_use_ble_central_client_mode),
            modifier = listSpacingModifier.fillMaxWidth(),
            isChecked = transferOptionsViewModel.presentmentBleCentralClientModeEnabled.collectAsState().value,
            onCheckedChange = { transferOptionsViewModel.setPresentmentBleCentralClientModeEnabled(it) }
        )
        SettingSwitch(
            label = stringResource(Res.string.switch_label_use_ble_peripheral_server_mode),
            modifier = listSpacingModifier.fillMaxWidth(),
            isChecked = transferOptionsViewModel.presentmentBlePeripheralServerModeEnabled.collectAsState().value,
            onCheckedChange = { transferOptionsViewModel.setPresentmentBlePeripheralServerModeEnabled(it) }
        )
        SettingSwitch(
            label = stringResource(Res.string.switch_label_use_nfc_data_transfer),
            modifier = listSpacingModifier.fillMaxWidth(),
            isChecked = transferOptionsViewModel.presentmentNfcDataTransferEnabled.collectAsState().value,
            onCheckedChange = { transferOptionsViewModel.setPresentmentNfcDataTransferEnabled(it) }
        )
        SettingSwitch(
            label = stringResource(Res.string.switch_label_blel2cap_enabled),
            modifier = listSpacingModifier.fillMaxWidth(),
            isChecked = transferOptionsViewModel.readerBleL2CapEnabled.collectAsState().value,
            onCheckedChange = { transferOptionsViewModel.setReaderBleL2CapEnabled(it) }
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
