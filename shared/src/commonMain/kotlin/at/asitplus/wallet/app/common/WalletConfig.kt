package at.asitplus.wallet.app.common

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
    val dataStoreService: DataStoreService
) {
    init {
        loadConfig()
    }
    fun loadConfig() {
        val input = runBlocking{dataStoreService.getData(Resources.DATASTORE_KEY_CONFIG)}
        if (input == null){
            this.host = "https://wallet.a-sit.at"
        } else{
            val config = jsonSerializer.decodeFromString<ConfigData>(input)
            this.host = config.host
        }
    }

    fun exportConfig() {
        val config = ConfigData(host = this.host)
        runBlocking {dataStoreService.setData(jsonSerializer.encodeToString(config), Resources.DATASTORE_KEY_CONFIG)}
    }

}

/**
 * Data class which holds the wallet preferences
 */
@Serializable
data class ConfigData(var host: String)

