package data.storage

import Resources
import androidx.compose.runtime.mutableStateOf
import at.asitplus.KmmResult
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.jsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) : SubjectCredentialStore {
    val credentialSize = mutableStateOf(0)
    private val container = importFromDataStore()

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme
    ) {
        container.credentials += SubjectCredentialStore.StoreEntry.Vc(vcSerialized, vc, scheme)
        exportToDataStore()
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ) {
        container.credentials += SubjectCredentialStore.StoreEntry.SdJwt(vcSerialized, vc, disclosures, scheme)
        exportToDataStore()
    }

    override suspend fun storeCredential(issuerSigned: IssuerSigned, scheme: ConstantIndex.CredentialScheme) {
        container.credentials += SubjectCredentialStore.StoreEntry.Iso(issuerSigned, scheme)
        exportToDataStore()
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
        container.attachments += SubjectCredentialStore.AttachmentEntry(name, data, vcId)
        exportToDataStore()
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<ConstantIndex.CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        return credentialSchemes?.let { schemes ->
            KmmResult.success(container.credentials.filter {
                when (it) {
                    is SubjectCredentialStore.StoreEntry.Iso -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.Vc -> it.scheme in schemes
                }
            }.toList())
        } ?: KmmResult.success(container.credentials)
    }

    override suspend fun getAttachment(name: String) =
        container.attachments.firstOrNull { it.name == name }?.data?.let { KmmResult.success(it) }
            ?: KmmResult.failure(NullPointerException("Attachment not found"))

    override suspend fun getAttachment(name: String, vcId: String) =
        container.attachments.firstOrNull { it.name == name && it.vcId == vcId }?.data?.let { KmmResult.success(it) }
            ?: KmmResult.failure(NullPointerException("Attachment not found"))

    private suspend fun exportToDataStore(){
        val exportableCredentials = mutableListOf<ExportableStoreEntry>()
        container.credentials.forEach {
            val scheme: ExportableCredentialScheme
            when (it) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    scheme = when (it.scheme){
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(ExportableStoreEntry.Iso(issuerSigned = it.issuerSigned, scheme = scheme))
                }
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    scheme = when (it.scheme){
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(ExportableStoreEntry.SdJwt(vcSerialized = it.vcSerialized, sdJwt = it.sdJwt, disclosures = it.disclosures, scheme = scheme))
                }
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    scheme = when (it.scheme){
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(ExportableStoreEntry.Vc(vcSerialized = it.vcSerialized, vc = it.vc, scheme = scheme))
                }
            }
        }

        val exportableAttachments= mutableListOf<ExportableAttachmentEntry>()
        container.attachments.forEach {
            exportableAttachments.add(ExportableAttachmentEntry(name = it.name, data = it.data, vcId = it.vcId))
        }

        val exportableContainer = ExportableStoreContainer(exportableCredentials, exportableAttachments)

        val json = jsonSerializer.encodeToString(exportableContainer)
        dataStore.setPreference(key = Resources.DATASTORE_KEY_VCS, value = json)
        credentialSize.value = container.credentials.size
    }

    private fun importFromDataStore(): StoreContainer{
        return runBlocking {
            val input = dataStore.getPreference(Resources.DATASTORE_KEY_VCS)
            if (input == null){
                StoreContainer(credentials = mutableListOf(), attachments = mutableListOf())
            } else {
                val export: ExportableStoreContainer = jsonSerializer.decodeFromString(input)
                val credentials = mutableListOf<SubjectCredentialStore.StoreEntry>()
                val attachments = mutableListOf<SubjectCredentialStore.AttachmentEntry>()

                export.credentials.forEach {
                    val scheme: ConstantIndex.CredentialScheme
                    when (it) {
                        is ExportableStoreEntry.Iso -> {
                            scheme = when (it.scheme){
                                ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                                ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                                ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            }
                            credentials.add(SubjectCredentialStore.StoreEntry.Iso(it.issuerSigned, scheme))
                        }
                        is ExportableStoreEntry.SdJwt -> {
                            scheme = when (it.scheme){
                                ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                                ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                                ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            }
                            credentials.add(SubjectCredentialStore.StoreEntry.SdJwt(vcSerialized = it.vcSerialized, sdJwt = it.sdJwt, disclosures = it.disclosures, scheme = scheme))
                        }
                        is ExportableStoreEntry.Vc -> {
                            scheme = when (it.scheme){
                                ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                                ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                                ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            }
                            credentials.add(SubjectCredentialStore.StoreEntry.Vc(vcSerialized = it.vcSerialized, vc = it.vc, scheme = scheme))
                        }
                    }
                }
                export.attachments.forEach {
                    attachments.add(SubjectCredentialStore.AttachmentEntry(name = it.name, data = it.data, vcId = it.vcId))
                }
                credentialSize.value = credentials.size
                StoreContainer(credentials, attachments)
            }
        }
    }

    suspend fun reset(){
        container.credentials.clear()
        container.attachments.clear()
        exportToDataStore()
    }

    suspend fun removeStoreEntryById(id: String) {
        container.credentials.forEach { entry ->
            when (entry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    if (entry.vc.jwtId == id) {
                        container.credentials.remove(entry)
                    }
                }
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    if (entry.sdJwt.jwtId == id) {
                        container.credentials.remove(entry)
                    }
                }
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    TODO()
                }
            }
        }
        exportToDataStore()
    }

     fun getStoreEntryById(id: String): SubjectCredentialStore.StoreEntry? {
         container.credentials.forEach { entry ->
            when (entry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    if (entry.vc.jwtId == id) {
                        return entry
                    }
                }
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    if (entry.sdJwt.jwtId == id) {
                        return entry
                    }
                }
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    TODO()
                }
            }

        }
        return null
    }

    fun getStoreEntries(): MutableList<SubjectCredentialStore.StoreEntry> {
        return container.credentials
    }

    fun getCredentialSize(): Int {
        return container.credentials.size
    }

}

data class StoreContainer(val credentials: MutableList<SubjectCredentialStore.StoreEntry>, val attachments: MutableList<SubjectCredentialStore.AttachmentEntry>)
@Serializable
data class ExportableStoreContainer(val credentials: MutableList<ExportableStoreEntry>, val attachments: MutableList<ExportableAttachmentEntry>)

@Serializable
sealed class ExportableStoreEntry {
    @Serializable
    data class Vc(
        val vcSerialized: String,
        val vc: VerifiableCredentialJws,
        val scheme: ExportableCredentialScheme
    ) : ExportableStoreEntry()
    @Serializable
    data class SdJwt(
        val vcSerialized: String,
        val sdJwt: VerifiableCredentialSdJwt,
        /**
         * Map of original serialized disclosure item to parsed item
         */
        val disclosures: Map<String, SelectiveDisclosureItem?>,
        val scheme: ExportableCredentialScheme
    ) : ExportableStoreEntry()
    @Serializable
    data class Iso(
        val issuerSigned: IssuerSigned,
        val scheme: ExportableCredentialScheme
    ) : ExportableStoreEntry()
}

@Serializable
data class ExportableAttachmentEntry(val name: String, val data: ByteArray, val vcId: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExportableAttachmentEntry

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (vcId != other.vcId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + vcId.hashCode()
        return result
    }
}

enum class ExportableCredentialScheme {
    AtomicAttribute2023, IdAustriaScheme, MobileDrivingLicence2023

}