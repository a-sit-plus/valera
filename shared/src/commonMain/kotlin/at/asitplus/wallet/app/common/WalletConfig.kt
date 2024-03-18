package at.asitplus.wallet.app.common

import ErrorService
import Configuration
import at.asitplus.wallet.lib.data.ConstantIndex
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
    private val config: Flow<ConfigData> = dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).map {
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

    val credentialRepresentation: Flow<ConstantIndex.CredentialRepresentation> = config.map {
        it.credentialRepresentation
    }

    fun set(
        host: String? = null,
        isConditionsAccepted: Boolean? = null,
        credentialRepresentation: ConstantIndex.CredentialRepresentation? = null,
    ) {
        try {
            runBlocking {
                val newConfig = ConfigData(
                    host = host ?: this@WalletConfig.host.first(),
                    isConditionsAccepted = isConditionsAccepted
                        ?: this@WalletConfig.isConditionsAccepted.first(),
                    credentialRepresentation = credentialRepresentation
                        ?: this@WalletConfig.credentialRepresentation.first(),
                )

                dataStoreService.setPreference(
                    jsonSerializer.encodeToString(newConfig),
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
data class ConfigData(
    val host: String,
    val credentialRepresentation: ConstantIndex.CredentialRepresentation = ConstantIndex.CredentialRepresentation.SD_JWT,
    val isConditionsAccepted: Boolean = false
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at",
    credentialRepresentation = ConstantIndex.CredentialRepresentation.SD_JWT,
    isConditionsAccepted = false,
)