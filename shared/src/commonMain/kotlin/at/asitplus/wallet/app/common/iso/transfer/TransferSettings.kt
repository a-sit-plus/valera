package at.asitplus.wallet.app.common.iso.transfer

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
    val readerAutomaticallySelectTransport: Boolean = true
)
