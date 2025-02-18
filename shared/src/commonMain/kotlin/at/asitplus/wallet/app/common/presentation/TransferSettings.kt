package at.asitplus.wallet.app.common.presentation

data class TransferSettings(
    val presentmentNegotiatedHandoverPreferredOrder: List<String> = listOf(
        //"ble:central_client_mode:",
        //"ble:peripheral_server_mode:",
        "nfc:"
    ),
    val presentmentUseNegotiatedHandover: Boolean = true,
    val presentmentBleCentralClientModeEnabled: Boolean = false,
    val presentmentBlePeripheralServerModeEnabled: Boolean = false,
    val presentmentNfcDataTransferEnabled: Boolean = true,
    val readerBleL2CapEnabled: Boolean = false,
    val presentmentAllowMultipleRequests: Boolean = false,
)
