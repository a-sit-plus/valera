package at.asitplus.wallet.app.common

import ErrorService
import Resources
import at.asitplus.wallet.lib.data.jsonSerializer
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
    private val config: Flow<ConfigData> = dataStoreService.getData(Resources.DATASTORE_KEY_CONFIG).map {
        it?.let {
            jsonSerializer.decodeFromString<ConfigData>(it)
        } ?: ConfigDataDefaults
    }

    val host: Flow<String> = config.map {
        it.host
    }

    val isConditionsAccepted: Flow<Boolean> = config.map {
        it.isConditionsAccepted
    }

    fun set(
        host: String? = null,
        isConditionsAccepted: Boolean? = null,
    ) {
        try {
            runBlocking {
                val newConfig = ConfigData(
                    host = host ?: this@WalletConfig.host.first(),
                    isConditionsAccepted = isConditionsAccepted ?: this@WalletConfig.isConditionsAccepted.first(),
                )

                dataStoreService.setData(
                    jsonSerializer.encodeToString(newConfig),
                    Resources.DATASTORE_KEY_CONFIG
                )
            }
        } catch (e: Exception) {
            errorService.emit(e)
        }
    }

    suspend fun reset() {
        dataStoreService.deleteData(Resources.DATASTORE_KEY_CONFIG)
    }
}

/**
 * Data class which holds the wallet preferences
 */
@Serializable
data class ConfigData(
    val host: String,
    val isConditionsAccepted: Boolean,
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at",
    isConditionsAccepted = false,
)