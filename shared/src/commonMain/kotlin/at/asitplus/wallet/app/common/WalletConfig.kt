package at.asitplus.wallet.app.common

import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Class to hand over to services like ProvisioningService so we can always retrieve the current config (e.g. url to Issuing Service)
 */
class WalletConfig(
    val dataStoreService: DataStoreService,
    val errorService: ErrorService
) : SettingsRepository {
    private val config: Flow<ConfigData> =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).map {
            it?.let { vckJsonSerializer.decodeFromString<ConfigData>(it) }
                ?: ConfigDataDefaults
        }

    override val host: Flow<String> = config.map {
        // Rewrite old, incompatible issuing service to new instance
        if (it.host == "https://wallet.a-sit.at/m5") "https://wallet.a-sit.at/m6" else it.host
    }

    override val isConditionsAccepted: Flow<Boolean> = config.map { it.isConditionsAccepted }
    override val presentmentUseNegotiatedHandover: Flow<Boolean> = config.map { it.presentmentUseNegotiatedHandover }
    override val presentmentBleCentralClientModeEnabled: Flow<Boolean> = config.map { it.presentmentBleCentralClientModeEnabled }
    override val presentmentBlePeripheralServerModeEnabled: Flow<Boolean> =
        config.map { it.presentmentBlePeripheralServerModeEnabled }
    override val presentmentNfcDataTransferEnabled: Flow<Boolean> = config.map { it.presentmentNfcDataTransferEnabled }
    override val readerBleL2CapEnabled: Flow<Boolean> = config.map { it.readerBleL2CapEnabled }
    override val presentmentAllowMultipleRequests: Flow<Boolean> = config.map { it.presentmentAllowMultipleRequests }
    override val readerAutomaticallySelectTransport: Flow<Boolean> = config.map { it.readerAutomaticallySelectTransport }
    override val connectionTimeout: Flow<Duration> = config.map { it.connectionTimeout }

    override fun set(
        host: String?,
        isConditionsAccepted: Boolean?,
        presentmentUseNegotiatedHandover: Boolean?,
        presentmentBleCentralClientModeEnabled: Boolean?,
        presentmentBlePeripheralServerModeEnabled: Boolean?,
        presentmentNfcDataTransferEnabled: Boolean?,
        readerBleL2CapEnabled: Boolean?,
        presentmentAllowMultipleRequests: Boolean?,
        readerAutomaticallySelectTransport: Boolean?,
        connectionTimeout: Duration?,
        completionHandler: CompletionHandler
    ): Result<Unit> = runCatching {
        runBlocking {
            val newConfig = ConfigData(
                host = host ?: this@WalletConfig.host.first(),
                isConditionsAccepted = isConditionsAccepted ?: this@WalletConfig.isConditionsAccepted.first(),
                presentmentUseNegotiatedHandover = presentmentUseNegotiatedHandover
                    ?: this@WalletConfig.presentmentUseNegotiatedHandover.first(),
                presentmentBleCentralClientModeEnabled = presentmentBleCentralClientModeEnabled
                    ?: this@WalletConfig.presentmentBleCentralClientModeEnabled.first(),
                presentmentBlePeripheralServerModeEnabled = presentmentBlePeripheralServerModeEnabled
                    ?: this@WalletConfig.presentmentBlePeripheralServerModeEnabled.first(),
                presentmentNfcDataTransferEnabled = presentmentNfcDataTransferEnabled
                    ?: this@WalletConfig.presentmentNfcDataTransferEnabled.first(),
                readerBleL2CapEnabled = readerBleL2CapEnabled ?: this@WalletConfig.readerBleL2CapEnabled.first(),
                presentmentAllowMultipleRequests = presentmentAllowMultipleRequests
                    ?: this@WalletConfig.presentmentAllowMultipleRequests.first(),
                readerAutomaticallySelectTransport = readerAutomaticallySelectTransport
                    ?: this@WalletConfig.readerAutomaticallySelectTransport.first(),
                connectionTimeout = connectionTimeout ?: this@WalletConfig.connectionTimeout.first(),
            )

            dataStoreService.setPreference(
                vckJsonSerializer.encodeToString(newConfig),
                Configuration.DATASTORE_KEY_CONFIG
            )
        }
    }.onFailure {
        errorService.emit(it)
    }

    override val presentmentNegotiatedHandoverPreferredOrder: List<String> = listOf(
        BLE_CENTRAL_CLIENT_MODE,
        BLE_PERIPHERAL_SERVER_MODE,
        NFC_DATA_TRANSFER
    )

    override suspend fun isConnectionMethodEnabled(prefix: String): Boolean =
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

    override suspend fun reset() {
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_CONFIG)
    }

    companion object {
        private const val BLE_CENTRAL_CLIENT_MODE = "ble:central_client_mode:"
        private const val BLE_PERIPHERAL_SERVER_MODE = "ble:peripheral_server_mode:"
        private const val NFC_DATA_TRANSFER = "nfc:"
    }
}

/**
 * Data class which holds the wallet preferences
 */
@Serializable
private data class ConfigData(
    val host: String,
    val isConditionsAccepted: Boolean = false,
    val presentmentUseNegotiatedHandover: Boolean = true,
    val presentmentBleCentralClientModeEnabled: Boolean = true,
    val presentmentBlePeripheralServerModeEnabled: Boolean = true,
    val presentmentNfcDataTransferEnabled: Boolean = false,
    val readerBleL2CapEnabled: Boolean = true,
    val presentmentAllowMultipleRequests: Boolean = false,
    val readerAutomaticallySelectTransport: Boolean = true,
    val connectionTimeout: Duration = 15.seconds,
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at/m6",
    isConditionsAccepted = false,
)