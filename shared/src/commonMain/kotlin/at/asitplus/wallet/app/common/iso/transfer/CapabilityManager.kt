package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import ui.viewmodels.SettingsViewModel

object CapabilityManager {
    val tag = "CapabilityManager"
    private val bluetoothInfo = BluetoothInfo()
    private val nfcInfo = NfcInfo()

    @Composable
    fun isBluetoothEnabled(): Boolean {
        return bluetoothInfo.isBluetoothEnabled()
    }

    @Composable
    fun isNfcEnabled(): Boolean {
        return nfcInfo.isNfcEnabled()
    }

    @Composable
    fun isAnyTransferMethodAvailable(): Boolean {
        return (isBluetoothEnabled() || isNfcEnabled())
    }

    @Composable
    fun hasCapabilitiesForConfig(coroutineScope: CoroutineScope, settingsViewModel: SettingsViewModel): Boolean {
        Napier.d("Checking capabilities for config...", tag=tag)

        return isAnyTransferMethodAvailable()

        // TODO: this does not work yet - just return the info if any method is available at the moment
        val presentmentUseNegotiatedHandover = settingsViewModel.presentmentUseNegotiatedHandover.collectAsState(initial = false).value
        val presentmentNfcDataTransferEnabled = settingsViewModel.presentmentNfcDataTransferEnabled.collectAsState(initial = false).value
        val presentmentBleCentralClientModeEnabled = settingsViewModel.presentmentBleCentralClientModeEnabled.collectAsState(initial = false).value
        val presentmentBlePeripheralServerModeEnabled = settingsViewModel.presentmentBlePeripheralServerModeEnabled.collectAsState(initial = false).value

        Napier.d("presentmentUseNegotiatedHandover = $presentmentUseNegotiatedHandover", tag=tag)
        Napier.d("presentmentNfcDataTransferEnabled = $presentmentNfcDataTransferEnabled", tag=tag)
        Napier.d("presentmentBleCentralClientModeEnabled = $presentmentBleCentralClientModeEnabled", tag=tag)
        Napier.d("presentmentBlePeripheralServerModeEnabled = $presentmentBlePeripheralServerModeEnabled", tag=tag)

        // Negotiated handover and at least one method available
        if (presentmentUseNegotiatedHandover && isAnyTransferMethodAvailable()) {
            return true
        }

        // Static handover checks
        if (
            (presentmentNfcDataTransferEnabled && isNfcEnabled()) ||
            ((presentmentBleCentralClientModeEnabled || presentmentBlePeripheralServerModeEnabled) && isBluetoothEnabled())
        ) {
            return true
        }

        Napier.w("No matching capabilities for config are available", tag=tag)
        return false
    }
}
