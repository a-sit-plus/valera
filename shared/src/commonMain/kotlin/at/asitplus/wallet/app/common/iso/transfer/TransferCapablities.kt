package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

@Composable
fun isAnyTransferMethodAvailable(): Boolean {
    return (BluetoothInfo().isBluetoothEnabled() || NfcInfo().isNfcEnabled())
}
