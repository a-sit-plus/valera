package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val configMutex = Mutex()

    private val config: Flow<ConfigData> =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).map {
            it?.let { vckJsonSerializer.decodeFromString<ConfigData>(it) }
                ?: ConfigDataDefaults
        }

    override val host: Flow<String> = config.map {
        // Rewrite old issuing service to new instance
        if (it.host == "https://wallet.a-sit.at/m6") "https://wallet.a-sit.at/m7" else it.host
    }
    override val clientId: Flow<String> = config.map { it.clientId }

    override val isConditionsAccepted: Flow<Boolean> = config.map { it.isConditionsAccepted }
    override val presentmentUseNegotiatedHandover: Flow<Boolean> = config.map { it.presentmentUseNegotiatedHandover }
    override val presentmentBleCentralClientModeEnabled: Flow<Boolean> = config.map { it.presentmentBleCentralClientModeEnabled }
    override val presentmentBlePeripheralServerModeEnabled: Flow<Boolean> = config.map { it.presentmentBlePeripheralServerModeEnabled }
    override val presentmentNfcDataTransferEnabled: Flow<Boolean> = config.map { it.presentmentNfcDataTransferEnabled }
    override val bleUseL2CAPEnabled: Flow<Boolean> = config.map { it.bleUseL2CAPEnabled }
    override val bleUseL2CAPInEngagementEnabled: Flow<Boolean> = config.map { it.bleUseL2CAPInEngagementEnabled }
    override val presentmentAllowMultipleRequests: Flow<Boolean> = config.map { it.presentmentAllowMultipleRequests }
    override val readerAutomaticallySelectTransport: Flow<Boolean> = config.map { it.readerAutomaticallySelectTransport }
    override val connectionTimeout: Flow<Duration> = config.map { it.connectionTimeout }

    override fun setPresentmentBleEnabled(enabled: Boolean): KmmResult<Unit> =
        updateConfig { current ->
            if (!enabled) {
                current.copy(
                    presentmentBleCentralClientModeEnabled = false,
                    presentmentBlePeripheralServerModeEnabled = false,
                )
            } else {
                val restoreDefault = !current.presentmentBleCentralClientModeRemembered &&
                        !current.presentmentBlePeripheralServerModeRemembered
                val centralEnabled = if (restoreDefault) {
                    ConfigDataDefaults.presentmentBleCentralClientModeEnabled
                } else {
                    current.presentmentBleCentralClientModeRemembered
                }
                val peripheralEnabled = if (restoreDefault) {
                    ConfigDataDefaults.presentmentBlePeripheralServerModeEnabled
                } else {
                    current.presentmentBlePeripheralServerModeRemembered
                }
                current.copy(
                    presentmentBleCentralClientModeEnabled = centralEnabled,
                    presentmentBlePeripheralServerModeEnabled = peripheralEnabled,
                    presentmentBleCentralClientModeRemembered = centralEnabled,
                    presentmentBlePeripheralServerModeRemembered = peripheralEnabled,
                )
            }
        }

    override fun setPresentmentBleCentralClientModeEnabled(enabled: Boolean): KmmResult<Unit> =
        updateConfig { current ->
            current.copy(
                presentmentBleCentralClientModeEnabled = enabled,
                presentmentBleCentralClientModeRemembered = enabled,
                presentmentBlePeripheralServerModeRemembered = current.presentmentBlePeripheralServerModeEnabled,
            )
        }

    override fun setPresentmentBlePeripheralServerModeEnabled(enabled: Boolean): KmmResult<Unit> =
        updateConfig { current ->
            current.copy(
                presentmentBlePeripheralServerModeEnabled = enabled,
                presentmentBlePeripheralServerModeRemembered = enabled,
                presentmentBleCentralClientModeRemembered = current.presentmentBleCentralClientModeEnabled,
            )
        }

    override fun set(
        host: String?,
        clientId: String?,
        isConditionsAccepted: Boolean?,
        presentmentUseNegotiatedHandover: Boolean?,
        presentmentBleCentralClientModeEnabled: Boolean?,
        presentmentBlePeripheralServerModeEnabled: Boolean?,
        presentmentNfcDataTransferEnabled: Boolean?,
        bleUseL2CAPEnabled: Boolean?,
        bleUseL2CAPInEngagementEnabled: Boolean?,
        presentmentAllowMultipleRequests: Boolean?,
        readerAutomaticallySelectTransport: Boolean?,
        connectionTimeout: Duration?,
        completionHandler: CompletionHandler
    ): KmmResult<Unit> = updateConfig { current ->
        val bleModesProvided = presentmentBleCentralClientModeEnabled != null ||
                presentmentBlePeripheralServerModeEnabled != null
        val centralEnabled = presentmentBleCentralClientModeEnabled
            ?: current.presentmentBleCentralClientModeEnabled
        val peripheralEnabled = presentmentBlePeripheralServerModeEnabled
            ?: current.presentmentBlePeripheralServerModeEnabled

        current.copy(
                host = host ?: current.host,
                clientId = clientId ?: current.clientId,
                isConditionsAccepted = isConditionsAccepted ?: current.isConditionsAccepted,
                presentmentUseNegotiatedHandover = presentmentUseNegotiatedHandover
                    ?: current.presentmentUseNegotiatedHandover,
                presentmentBleCentralClientModeEnabled = centralEnabled,
                presentmentBlePeripheralServerModeEnabled = peripheralEnabled,
                presentmentBleCentralClientModeRemembered = if (bleModesProvided) {
                    centralEnabled
                } else {
                    current.presentmentBleCentralClientModeRemembered
                },
                presentmentBlePeripheralServerModeRemembered = if (bleModesProvided) {
                    peripheralEnabled
                } else {
                    current.presentmentBlePeripheralServerModeRemembered
                },
                presentmentNfcDataTransferEnabled = presentmentNfcDataTransferEnabled
                    ?: current.presentmentNfcDataTransferEnabled,
                bleUseL2CAPEnabled = bleUseL2CAPEnabled ?: current.bleUseL2CAPEnabled,
                bleUseL2CAPInEngagementEnabled = bleUseL2CAPInEngagementEnabled
                    ?: current.bleUseL2CAPInEngagementEnabled,
                presentmentAllowMultipleRequests = presentmentAllowMultipleRequests
                    ?: current.presentmentAllowMultipleRequests,
                readerAutomaticallySelectTransport = readerAutomaticallySelectTransport
                    ?: current.readerAutomaticallySelectTransport,
                connectionTimeout = connectionTimeout ?: current.connectionTimeout,
            )
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

    private fun updateConfig(transform: (ConfigData) -> ConfigData): KmmResult<Unit> =
        try {
            runBlocking {
                configMutex.withLock {
                    val current = readConfigData()
                    val updated = transform(current)
                    dataStoreService.setPreference(
                        vckJsonSerializer.encodeToString(updated),
                        Configuration.DATASTORE_KEY_CONFIG
                    )
                }
            }
            KmmResult.success(Unit)
        } catch (error: Throwable) {
            errorService.emit(error)
            KmmResult.failure(error)
        }

    private suspend fun readConfigData(): ConfigData =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).first()
            ?.let { vckJsonSerializer.decodeFromString<ConfigData>(it) }
            ?: ConfigDataDefaults

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
    val clientId: String = SettingsRepository.DEFAULT_CLIENT_ID,
    val isConditionsAccepted: Boolean = false,
    val presentmentUseNegotiatedHandover: Boolean = true,
    val presentmentBleCentralClientModeEnabled: Boolean = true,
    val presentmentBlePeripheralServerModeEnabled: Boolean = true,
    val presentmentBleCentralClientModeRemembered: Boolean = true,
    val presentmentBlePeripheralServerModeRemembered: Boolean = true,
    val presentmentNfcDataTransferEnabled: Boolean = false,
    val bleUseL2CAPEnabled: Boolean = true,
    val bleUseL2CAPInEngagementEnabled: Boolean = true,
    val presentmentAllowMultipleRequests: Boolean = false,
    val readerAutomaticallySelectTransport: Boolean = true,
    val connectionTimeout: Duration = 15.seconds,
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at/m7",
    clientId = SettingsRepository.DEFAULT_CLIENT_ID,
    isConditionsAccepted = false,
)
