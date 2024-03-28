package at.asitplus.wallet.app.common

import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.AttributeIndex
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
    private val config: Flow<ConfigData> =
        dataStoreService.getPreference(Configuration.DATASTORE_KEY_CONFIG).map {
            it?.let {
                jsonSerializer.decodeFromString<ConfigData>(it)
            } ?: ConfigDataDefaults
        }

    val host: Flow<String> = config.map {
        it.host
    }

    val credentialRepresentation: Flow<ConstantIndex.CredentialRepresentation> = config.map {
        it.credentialRepresentation
    }

    val credentialScheme: Flow<ConstantIndex.CredentialScheme> = config.map {
        AttributeIndex.resolveAttributeType(it.credentialSchemeVcType) ?: throw Exception("Unsupported attribute type: $it")
    }

    val isConditionsAccepted: Flow<Boolean> = config.map {
        it.isConditionsAccepted
    }

    fun set(
        host: String? = null,
        credentialRepresentation: ConstantIndex.CredentialRepresentation? = null,
        credentialSchemeVcType: String? = null,
        isConditionsAccepted: Boolean? = null,
    ) {
        try {
            runBlocking {
                val newConfig = ConfigData(
                    host = host ?: this@WalletConfig.host.first(),
                    credentialSchemeVcType = credentialSchemeVcType
                        ?: this@WalletConfig.credentialScheme.first().vcType,
                    credentialRepresentation = credentialRepresentation
                        ?: this@WalletConfig.credentialRepresentation.first(),
                    isConditionsAccepted = isConditionsAccepted
                        ?: this@WalletConfig.isConditionsAccepted.first(),
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
private data class ConfigData(
    val host: String,
    val credentialRepresentation: ConstantIndex.CredentialRepresentation = ConstantIndex.CredentialRepresentation.SD_JWT,
    val credentialSchemeVcType: String = IdAustriaScheme.vcType,
    val isConditionsAccepted: Boolean = false
)

private val ConfigDataDefaults = ConfigData(
    host = "https://wallet.a-sit.at",
    credentialRepresentation = ConstantIndex.CredentialRepresentation.SD_JWT,
    credentialSchemeVcType = IdAustriaScheme.vcType,
    isConditionsAccepted = false
)