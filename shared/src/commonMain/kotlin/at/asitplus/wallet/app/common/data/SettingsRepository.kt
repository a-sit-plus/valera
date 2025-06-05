package at.asitplus.wallet.app.common.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Duration

interface SettingsRepository {
    val host: Flow<String>
    val isConditionsAccepted: Flow<Boolean>
    val presentmentUseNegotiatedHandover: Flow<Boolean>
    val presentmentBleCentralClientModeEnabled: Flow<Boolean>
    val presentmentBlePeripheralServerModeEnabled: Flow<Boolean>
    val presentmentNfcDataTransferEnabled: Flow<Boolean>
    val readerBleL2CapEnabled: Flow<Boolean>
    val presentmentAllowMultipleRequests: Flow<Boolean>
    val readerAutomaticallySelectTransport: Flow<Boolean>
    val connectionTimeout: Flow<Duration>

    val presentmentNegotiatedHandoverPreferredOrder: List<String>
        get() = listOf(
            BLE_CENTRAL_CLIENT_MODE,
            BLE_PERIPHERAL_SERVER_MODE,
            NFC_DATA_TRANSFER
        )

    fun set(
        host: String? = null,
        isConditionsAccepted: Boolean? = null,
        presentmentUseNegotiatedHandover: Boolean? = null,
        presentmentBleCentralClientModeEnabled: Boolean? = null,
        presentmentBlePeripheralServerModeEnabled: Boolean? = null,
        presentmentNfcDataTransferEnabled: Boolean? = null,
        readerBleL2CapEnabled: Boolean? = null,
        presentmentAllowMultipleRequests: Boolean? = null,
        readerAutomaticallySelectTransport: Boolean? = null,
        connectionTimeout: Duration? = null,
        completionHandler: CompletionHandler = {},
    ): Result<Unit>

    suspend fun isConnectionMethodEnabled(prefix: String): Boolean =
        if (prefix.startsWith(BLE_CENTRAL_CLIENT_MODE)) {
            presentmentBleCentralClientModeEnabled.first()
        } else if (prefix.startsWith(BLE_PERIPHERAL_SERVER_MODE)) {
            presentmentBlePeripheralServerModeEnabled.first()
        } else if (prefix.startsWith(NFC_DATA_TRANSFER)) {
            presentmentNfcDataTransferEnabled.first()
        } else {
            Napier.w("Connection method $prefix is unknown")
            false
        }

    suspend fun reset()

    companion object {
        private const val BLE_CENTRAL_CLIENT_MODE = "ble:central_client_mode:"
        private const val BLE_PERIPHERAL_SERVER_MODE = "ble:peripheral_server_mode:"
        private const val NFC_DATA_TRANSFER = "nfc:"
    }
}