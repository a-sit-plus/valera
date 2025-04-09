package at.asitplus.wallet.app.common.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TransferSettings private constructor(
    val presentmentNegotiatedHandoverPreferredOrder: List<String> = listOf(
        "ble:central_client_mode:",
        "ble:peripheral_server_mode:",
        "nfc:"
    ),
    var presentmentUseNegotiatedHandover: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var presentmentBleCentralClientModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var presentmentBlePeripheralServerModeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var presentmentNfcDataTransferEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    var readerBleL2CapEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    val presentmentAllowMultipleRequests: Boolean = false,
    val readerAutomaticallySelectTransport: Boolean = true,
    val connectionTimeout: Duration = 15.seconds
) {
    companion object {
        val transferSettings: TransferSettings by lazy {
            TransferSettings()
        }
    }
}