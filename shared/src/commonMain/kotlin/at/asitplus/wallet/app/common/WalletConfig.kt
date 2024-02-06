package at.asitplus.wallet.app.common

import ErrorService
import Resources
import at.asitplus.wallet.lib.data.jsonSerializer
import data.storage.DataStoreService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Class to hand over to services like ProvisioningService so we can always retrieve the current config (e.g. url to Issuing Service)
 */
class WalletConfig(
    var host: String = "https://wallet.a-sit.at",
    val dataStoreService: DataStoreService,
    val errorService: ErrorService
) {
    init {
        loadConfig()
    }
    fun loadConfig() {
        try {
            val input = runBlocking{dataStoreService.getPreference(Resources.DATASTORE_KEY_CONFIG)}
            if (input == null){
                this.host = "https://wallet.a-sit.at"
            } else{
                val config = jsonSerializer.decodeFromString<ConfigData>(input)
                this.host = config.host
            }
        } catch (e: Throwable) {
            errorService.emit(e)
        }
    }

    fun exportConfig() {
        val config = ConfigData(host = this.host)
        try {
            runBlocking {dataStoreService.setPreference(jsonSerializer.encodeToString(config), Resources.DATASTORE_KEY_CONFIG)}
        } catch (e: Throwable) {
            errorService.emit(e)
        }

    }

}

/**
 * Data class which holds the wallet preferences
 */
@Serializable
data class ConfigData(var host: String)

