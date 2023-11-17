package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredential
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) : SubjectCredentialStore {
    private val dataKey = "VCs"
    private val idHolder: IdHolder = runBlocking { importFromDataStore() }

    override suspend fun getAttachment(name: String): KmmResult<ByteArray> {
        return KmmResult(ByteArray(0))
    }

    override suspend fun getAttachment(name: String, vcId: String): KmmResult<ByteArray> {
        return KmmResult(ByteArray(0))
    }

    override suspend fun getCredentials(requiredAttributeTypes: Collection<String>?): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val filtered = idHolder.credentials
            .filter { it ->
                val vc = it.vc
                requiredAttributeTypes?.let { types ->
                    vc.type.any { it in types }
                } ?: true
            }

        val approved = getCredentialsInternal(requiredAttributeTypes)
        return KmmResult.success(
            filtered.filter { it ->
                val vc = it.vc
                vc.type.any { approved.contains(it) }
            }
                .map {
                    it.let { vc ->
                        SubjectCredentialStore.StoreEntry.Vc(
                            vc.serialize(),
                            vc = vc,
                            scheme = at.asitplus.wallet.idaustria.ConstantIndex.IdAustriaCredential
                        )
                    }
                }
                .toList())
    }

    private fun getCredentialsInternal(
        requiredAttributeTypes: Collection<String>?
    ): Collection<String> {
        val content = requiredAttributeTypes
            ?: idHolder.credentials.map {
                val vc = it.vc
                (vc.type).toList()
            }.flatten()
                .filter { it != "NULL" }
                .filter { it != "VerifiableCredential" }
                .distinct().toList()
        return content
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme
    ) {
        Napier.d("storing $vcSerialized")
        idHolder.credentials.add(vc)
        exportToDataStore()
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme
    ) {
        TODO("Not yet implemented")
    }

    private suspend fun exportToDataStore() {
        runBlocking {
            val json = jsonSerializer.encodeToString(idHolder)
            dataStore.setData(value = json, key = dataKey)
        }
    }

    private suspend fun importFromDataStore(): IdHolder {
        val input = dataStore.getData(dataKey)
        return jsonSerializer.decodeFromString(input.toString()) ?: IdHolder()
    }

    suspend fun removeCredential(id: String) {
        var found: VerifiableCredentialJws? = null
        idHolder.credentials.forEach {
            val vc = it.vc
            if (vc.id == id) {
                found = it
            }
        }
        if (found != null) {
            idHolder.credentials.remove(found)
            exportToDataStore()
        }
    }

    suspend fun getVcs(): ArrayList<VerifiableCredential> {
        val credentialList = ArrayList<VerifiableCredential>()
        val storeEntries = getCredentials(null).getOrThrow()
        storeEntries.forEach { entry ->
            when (entry) {
                is SubjectCredentialStore.StoreEntry.Iso -> TODO()
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    credentialList.add(entry.vc.vc)
                }

                else -> {}
            }
        }
        return credentialList
    }

    suspend fun getCredentialById(id: String): VerifiableCredential? {
        val storeEntries = getCredentials(null).getOrThrow()
        storeEntries.forEach { entry ->
            if (entry is SubjectCredentialStore.StoreEntry.Vc) {
                if (entry.vc.vc.id == id) {
                    return entry.vc.vc
                }
            }
        }
        return null
    }
}