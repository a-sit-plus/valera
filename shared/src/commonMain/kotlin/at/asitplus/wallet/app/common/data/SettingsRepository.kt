package at.asitplus.wallet.app.common.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Duration

/**
 * Settings provided by the application.
 *
 * Proximity mDoc transport options according to ISO/IEC 18013-5:2021:
 * @property presentmentUseNegotiatedHandover if `true` NFC negotiated handover will be used, otherwise NFC static handover.
 * @property presentmentBleCentralClientModeEnabled `true` if mdoc BLE Central Client mode should be offered.
 * @property presentmentBlePeripheralServerModeEnabled true` if mdoc BLE Peripheral Server mode should be offered.
 * @property presentmentNfcDataTransferEnabled `true` if NFC data transfer should be offered.
 * @property presentmentAllowMultipleRequests whether to allow multiple requests.
 * @property readerAutomaticallySelectTransport whether the reader automatically select the transport method
 * @property bleUseL2CAPEnabled set to `true` to use BLE L2CAP if available, `false` otherwise.
 * @property bleUseL2CAPInEngagementEnabled set to `true` to use BLE L2CAP from the engagement, `false` otherwise.
 * @property connectionTimeout the timeout for closing a connection.
 * @property presentmentNegotiatedHandoverPreferredOrder a list specifying the preferred order of transport methods to use when creating an NFC negotiated handover.
 */

interface SettingsRepository {
    val host: Flow<String>
    val isConditionsAccepted: Flow<Boolean>
    val presentmentUseNegotiatedHandover: Flow<Boolean>
    val presentmentBleCentralClientModeEnabled: Flow<Boolean>
    val presentmentBlePeripheralServerModeEnabled: Flow<Boolean>
    val presentmentNfcDataTransferEnabled: Flow<Boolean>
    val bleUseL2CAPEnabled: Flow<Boolean>
    val bleUseL2CAPInEngagementEnabled: Flow<Boolean>
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
        bleUseL2CAPEnabled: Boolean? = null,
        bleUseL2CAPInEngagementEnabled: Boolean? = null,
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

    suspend fun awaitPresentmentSettingsFirst() {
        combine(
            presentmentBleCentralClientModeEnabled,
            presentmentBlePeripheralServerModeEnabled,
            presentmentNfcDataTransferEnabled
        ) { _, _, _ -> }.first()
    }

    suspend fun reset()

    companion object {
        private const val BLE_CENTRAL_CLIENT_MODE = "ble:central_client_mode:"
        private const val BLE_PERIPHERAL_SERVER_MODE = "ble:peripheral_server_mode:"
        private const val NFC_DATA_TRANSFER = "nfc:"
    }
}