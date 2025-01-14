package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Class to hand over to services like ProvisioningService so we can always retrieve the current config (e.g. url to Issuing Service)
 */
class WalletConfig(
    val dataStoreService: DataStoreService,
    val errorService: ErrorService
) {
    private val config: Flow<ConfigData> =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).map {
            it?.let { vckJsonSerializer.decodeFromString<ConfigData>(it) }
                ?: ConfigDataDefaults
        }

    val host: Flow<String> = config.map {
        it.host
    }

    val isConditionsAccepted: Flow<Boolean> = config.map {
        it.isConditionsAccepted
    }

    val qtspHost: Flow<String> = config.map {
        it.qtspHost
    }

    fun set(
        host: String? = null,
        isConditionsAccepted: Boolean? = null,
        qtspHost: String? = null
    ) {
        try {
            runBlocking {
                val newConfig = ConfigData(
                    host = host ?: this@WalletConfig.host.first(),
                    isConditionsAccepted = isConditionsAccepted ?: this@WalletConfig.isConditionsAccepted.first(),
                    qtspHost = qtspHost ?: this@WalletConfig.qtspHost.first()
                )

                dataStoreService.setPreference(
                    vckJsonSerializer.encodeToString(newConfig),
                    Configuration.DATASTORE_KEY_CONFIG
                )
            }
        } catch (e: Throwable) {
            errorService.emit(e)
        }
    }

    suspend fun reset() {
        dataStoreService.deletePreference(Configuration.DATASTORE_KEY_CONFIG)
    }
}

/**
 * Data class which holds the wallet preferences
 */
@Serializable
private data class ConfigData(
    val host: String,
    val isConditionsAccepted: Boolean = false,
    val qtspHost: String
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at/m5",
    isConditionsAccepted = false,
    qtspHost = "https://apps.egiz.gv.at/qtsp"
)