package data.storage

import DataStoreService
import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.AtomicAttribute2023
import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import data.idaustria.IdAustriaCredential
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

suspend fun getCredentials(storageService: SubjectCredentialStore): ArrayList<CredentialSubject> {
    val credentialList = ArrayList<CredentialSubject>()
    val storeEntries = storageService.getCredentials(null).getOrThrow()
    storeEntries.forEach {entry ->
        when(entry) {
            is SubjectCredentialStore.StoreEntry.Iso -> TODO()
            is SubjectCredentialStore.StoreEntry.Vc -> when (val subject = entry.vc.vc.credentialSubject) {
                is AtomicAttribute2023 -> {
                    credentialList.add(subject)
                }
                is IdAustriaCredential -> {
                    credentialList.add(subject)
                }
            }

            else -> {}
        }
    }
    return credentialList
}




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
                        SubjectCredentialStore.StoreEntry.Vc(vc.serialize(), vc)
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

    override suspend fun storeCredential(vc: VerifiableCredentialJws, vcSerialized: String) {
        Napier.d("storing $vcSerialized")
        // TODO CK analyze usage of attrName
        val attrName = (vc.vc.credentialSubject as? AtomicAttribute2023)?.name
            ?: "NULL"
        val attrTypes = vc.vc.type
        idHolder.credentials.add(vc)
        exportToDataStore()
    }

    override suspend fun storeCredential(issuerSigned: IssuerSigned) {
        println("TODO")
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
}