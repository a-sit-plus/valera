package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredential
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString


class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) :
    SubjectCredentialStore {
    private val container = this.observeStoreContainer()

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme,
    ): SubjectCredentialStore.StoreEntry {
        val storeEntry = SubjectCredentialStore.StoreEntry.Vc(
            vcSerialized,
            vc,
            scheme,
        )
        val newContainer = container.first().let {
            it.copy(it.credentials + listOf(storeEntry))
        }
        exportToDataStore(newContainer)
        return storeEntry
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ): SubjectCredentialStore.StoreEntry {
        val storeEntry = SubjectCredentialStore.StoreEntry.SdJwt(
            vcSerialized,
            vc,
            disclosures,
            scheme,
        )
        val newContainer = container.first().let {
            it.copy(it.credentials + listOf(storeEntry))
        }
        exportToDataStore(newContainer)
        return storeEntry
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned, scheme: ConstantIndex.CredentialScheme
    ): SubjectCredentialStore.StoreEntry {
        val storeEntry = SubjectCredentialStore.StoreEntry.Iso(issuerSigned, scheme)
        val newContainer = container.first().let {
            it.copy(it.credentials + listOf(storeEntry))
        }
        exportToDataStore(newContainer)
        return storeEntry
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<ConstantIndex.CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        return credentialSchemes?.let { schemes ->
            KmmResult.success(container.first().credentials.filter {
                when (it) {
                    is SubjectCredentialStore.StoreEntry.Iso -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.Vc -> it.scheme in schemes
                }
            }.toList())
        } ?: KmmResult.success(container.first().credentials)
    }

    private suspend fun exportToDataStore(newContainer: StoreContainer) {
        val json = jsonSerializer.encodeToString(newContainer)
        dataStore.setPreference(key = Configuration.DATASTORE_KEY_VCS, value = json)
    }

    suspend fun reset() {
        exportToDataStore(StoreContainer(credentials = listOf()))
    }

    suspend fun removeStoreEntryByIndex(index: Int) {
        val newContainer = container.first().let {
            it.copy(
                credentials = it.credentials.filterIndexed { credentialIndex, _ ->
                    credentialIndex != index
                },
            )
        }
        exportToDataStore(newContainer)
    }

    private fun dataStoreValueToStoreContainer(input: String?): StoreContainer {
        if (input == null) {
            return StoreContainer(credentials = mutableListOf())
        } else {
            return jsonSerializer.decodeFromString(input)
        }
    }

    fun observeStoreContainer(): Flow<StoreContainer> {
        return dataStore.getPreference(Configuration.DATASTORE_KEY_VCS).map {
            dataStoreValueToStoreContainer(it)
        }
    }
}

@Serializable
data class StoreContainer(
    val credentials: List<SubjectCredentialStore.StoreEntry>,
)
