package ui.views.iso.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_extended_options
import at.asitplus.valera.resources.section_heading_transfer_options
import at.asitplus.valera.resources.switch_label_ble_use_l2cap_enabled
import at.asitplus.valera.resources.switch_label_ble_use_l2cap_in_engagement_enabled
import at.asitplus.valera.resources.switch_label_use_ble
import at.asitplus.valera.resources.switch_label_use_ble_central_client_mode
import at.asitplus.valera.resources.switch_label_use_ble_peripheral_server_mode
import at.asitplus.valera.resources.switch_label_use_negotiated_handover
import at.asitplus.valera.resources.switch_label_use_nfc
import at.asitplus.valera.resources.switch_label_use_nfc_data_transfer
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.iso.common.TransferOptionsViewModel

@Composable
fun TransferOptionsView(
    transferOptionsViewModel: TransferOptionsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.section_heading_transfer_options),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        var showBluetoothOptions by remember { mutableStateOf(false) }

        val bleCentral = transferOptionsViewModel.presentmentBleCentralClientModeEnabled.collectAsState().value
        val blePeripheral = transferOptionsViewModel.presentmentBlePeripheralServerModeEnabled.collectAsState().value
        val bleUseL2CAP = transferOptionsViewModel.bleUseL2CAPEnabled.collectAsState().value
        val bleUseL2CAPInEngagement = transferOptionsViewModel.bleUseL2CAPInEngagementEnabled.collectAsState().value

        val bluetoothEnabled = bleCentral || blePeripheral

        GroupSwitch(
            title = stringResource(Res.string.switch_label_use_ble),
            isChecked = bluetoothEnabled,
            onCheckedChange = { enabled ->
                transferOptionsViewModel.setPresentmentBleCentralClientModeEnabled(enabled)
                transferOptionsViewModel.setPresentmentBlePeripheralServerModeEnabled(enabled)
            },
            onOptionalClick = { showBluetoothOptions = !showBluetoothOptions }
        )

        if (showBluetoothOptions) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_use_ble_central_client_mode),
                    isChecked = bleCentral,
                    onCheckedChange = { transferOptionsViewModel.setPresentmentBleCentralClientModeEnabled(it) }
                )
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_use_ble_peripheral_server_mode),
                    isChecked = blePeripheral,
                    onCheckedChange = { transferOptionsViewModel.setPresentmentBlePeripheralServerModeEnabled(it) }
                )
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_ble_use_l2cap_enabled),
                    isChecked = bleUseL2CAP,
                    onCheckedChange = { transferOptionsViewModel.setBleL2CAPEnabled(it) }
                )
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_ble_use_l2cap_in_engagement_enabled),
                    isChecked = bleUseL2CAPInEngagement,
                    onCheckedChange = { transferOptionsViewModel.setBleL2CAPInEngagementEnabled(it) }
                )
            }
        }

        var showNfcOptions by remember { mutableStateOf(false) }

        val nfcTransfer = transferOptionsViewModel.presentmentNfcDataTransferEnabled.collectAsState().value
        val nfcHandover = transferOptionsViewModel.presentmentUseNegotiatedHandover.collectAsState().value

        GroupSwitch(
            title = stringResource(Res.string.switch_label_use_nfc),
            isChecked = nfcTransfer,
            onCheckedChange = { enabled ->
                transferOptionsViewModel.setPresentmentNfcDataTransferEnabled(enabled)
            },
            onOptionalClick = { showNfcOptions = !showNfcOptions }
        )

        if (showNfcOptions) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_use_nfc_data_transfer),
                    isChecked = nfcTransfer,
                    onCheckedChange = { transferOptionsViewModel.setPresentmentNfcDataTransferEnabled(it) }
                )
                SettingSwitch(
                    label = stringResource(Res.string.switch_label_use_negotiated_handover),
                    isChecked = nfcHandover,
                    onCheckedChange = { transferOptionsViewModel.setPresentmentUseNegotiatedHandover(it) }
                )
            }
        }
    }
}

@Composable
private fun GroupSwitch(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOptionalClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOptionalClick) {
            Text(stringResource(Res.string.button_label_extended_options))
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
