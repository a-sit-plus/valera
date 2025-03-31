package at.asitplus.wallet.app.common.presentation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TransferSettings(
    val presentmentNegotiatedHandoverPreferredOrder: List<String> = listOf(
        "ble:central_client_mode:",
        "ble:peripheral_server_mode:",
        "nfc:"
    ),
    val presentmentUseNegotiatedHandover: Boolean = true,
    val presentmentBleCentralClientModeEnabled: Boolean = true,
    val presentmentBlePeripheralServerModeEnabled: Boolean = true,
    val presentmentNfcDataTransferEnabled: Boolean = true,
    val readerBleL2CapEnabled: Boolean = true,
    val presentmentAllowMultipleRequests: Boolean = false,
    val connectionTimeout: Duration = 15.seconds
)
