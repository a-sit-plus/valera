package at.asitplus.wallet.app.common.presentation

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TransferSettings private constructor(
    val presentmentNegotiatedHandoverPreferredOrder: List<String> = listOf(
        BLE_CENTRAL_CLIENT_MODE,
        BLE_PERIPHERAL_SERVER_MODE,
        NFC_DATA_TRANSFER
    ),
    var presentmentUseNegotiatedHandover: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var presentmentBleCentralClientModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var presentmentBlePeripheralServerModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false),
    var presentmentNfcDataTransferEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false),
    var readerBleL2CapEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    val presentmentAllowMultipleRequests: Boolean = false,
    val readerAutomaticallySelectTransport: Boolean = true,
    val connectionTimeout: Duration = 15.seconds
) {

    fun isConnectionMethodEnabled(prefix: String): Boolean {
        return if (prefix.startsWith(BLE_CENTRAL_CLIENT_MODE)) {
            presentmentBleCentralClientModeEnabled.value
        } else if (prefix.startsWith(BLE_PERIPHERAL_SERVER_MODE)) {
            presentmentBlePeripheralServerModeEnabled.value
        } else if (prefix.startsWith(NFC_DATA_TRANSFER)) {
            presentmentNfcDataTransferEnabled.value
        } else {
            Napier.w("Connection method $prefix is unknown")
            false
        }
    }

    companion object {
        private const val BLE_CENTRAL_CLIENT_MODE = "ble:central_client_mode:"
        private const val BLE_PERIPHERAL_SERVER_MODE = "ble:peripheral_server_mode:"
        private const val NFC_DATA_TRANSFER = "nfc:"
        val transferSettings: TransferSettings by lazy {
            TransferSettings()
        }
    }
}