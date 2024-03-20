package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
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
        scheme: ConstantIndex.CredentialScheme
    ) {
        val newContainer = container.first().let { containerInstance ->
            containerInstance.filterNotScheme(scheme).copy(
                credentials = listOf(
                    SubjectCredentialStore.StoreEntry.Vc(
                        vcSerialized,
                        vc,
                        scheme
                    )
                ),
                attachments = listOf(),
            )
        }
        exportToDataStore(newContainer)
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ) {
        val newContainer = container.first().let { containerInstance ->
            containerInstance.filterNotScheme(scheme).copy(
                credentials = listOf(
                    SubjectCredentialStore.StoreEntry.SdJwt(
                        vcSerialized,
                        vc,
                        disclosures,
                        scheme
                    ),
                ),
                attachments = listOf(),
            )
        }
        exportToDataStore(newContainer)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme
    ) {
        val newContainer = container.first().let { containerInstance ->
            containerInstance.filterNotScheme(scheme).let {
                it.copy(
                    credentials = listOf(
                        SubjectCredentialStore.StoreEntry.Iso(
                            issuerSigned,
                            scheme
                        ),
                    ),
                    attachments = listOf(),
                )
            }
        }
        exportToDataStore(newContainer)
    }

    override suspend fun storeAttachment(name: String, data: ByteArray, vcId: String) {
        val newContainer = container.first().let { storeContainer ->
            storeContainer.copy(
                credentials = storeContainer.credentials,
                // consider attachments unique by vcId and attachment name
                attachments = storeContainer.attachments.filterNot { attachment ->
                    attachment.vcId == vcId && attachment.name == name
                } + SubjectCredentialStore.AttachmentEntry(name, data, vcId),
            )
        }
        exportToDataStore(newContainer)
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

    override suspend fun getAttachment(name: String) =
        container.first().attachments.firstOrNull { it.name == name }?.data?.let {
            KmmResult.success(
                it
            )
        }
            ?: KmmResult.failure(NullPointerException("Attachment not found"))

    override suspend fun getAttachment(name: String, vcId: String) =
        container.first().attachments.firstOrNull { it.name == name && it.vcId == vcId }?.data?.let {
            KmmResult.success(
                it
            )
        }
            ?: KmmResult.failure(NullPointerException("Attachment not found"))

    private suspend fun exportToDataStore(newContainer: StoreContainer) {
        val exportableCredentials = mutableListOf<ExportableStoreEntry>()
        newContainer.credentials.forEach {
            val scheme: ExportableCredentialScheme
            when (it) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    scheme = when (it.scheme) {
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        EuPidScheme -> ExportableCredentialScheme.EuPidScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(
                        ExportableStoreEntry.Iso(
                            issuerSigned = it.issuerSigned,
                            scheme = scheme
                        )
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    scheme = when (it.scheme) {
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        EuPidScheme -> ExportableCredentialScheme.EuPidScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(
                        ExportableStoreEntry.SdJwt(
                            vcSerialized = it.vcSerialized,
                            sdJwt = it.sdJwt,
                            disclosures = it.disclosures,
                            scheme = scheme
                        )
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    scheme = when (it.scheme) {
                        ConstantIndex.AtomicAttribute2023 -> ExportableCredentialScheme.AtomicAttribute2023
                        ConstantIndex.MobileDrivingLicence2023 -> ExportableCredentialScheme.MobileDrivingLicence2023
                        IdAustriaScheme -> ExportableCredentialScheme.IdAustriaScheme
                        EuPidScheme -> ExportableCredentialScheme.EuPidScheme
                        else -> throw Exception("Unknown CredentialScheme")
                    }
                    exportableCredentials.add(
                        ExportableStoreEntry.Vc(
                            vcSerialized = it.vcSerialized,
                            vc = it.vc,
                            scheme = scheme
                        )
                    )
                }
            }
        }

        val exportableAttachments = mutableListOf<ExportableAttachmentEntry>()
        newContainer.attachments.forEach {
            exportableAttachments.add(
                ExportableAttachmentEntry(
                    name = it.name,
                    data = it.data,
                    vcId = it.vcId
                )
            )
        }

        val exportableContainer =
            ExportableStoreContainer(exportableCredentials, exportableAttachments)

        val json = jsonSerializer.encodeToString(exportableContainer)
        dataStore.setPreference(key = Configuration.DATASTORE_KEY_VCS, value = json)
    }

    suspend fun reset() {
        exportToDataStore(StoreContainer(credentials = listOf(), attachments = listOf()))
    }

    suspend fun removeStoreEntryById(id: String) {
        val newContainer = container.first().let {
            it.copy(
                credentials = it.credentials.filterNot { entry ->
                    when (entry) {
                        is SubjectCredentialStore.StoreEntry.Vc -> {
                            entry.vc.jwtId == id
                        }

                        is SubjectCredentialStore.StoreEntry.SdJwt -> {
                            entry.sdJwt.jwtId == id
                        }

                        is SubjectCredentialStore.StoreEntry.Iso -> {
                            false
                        }
                    }
                }
            )
        }
        exportToDataStore(newContainer)
    }

    fun observeStoreEntryById(id: String): Flow<SubjectCredentialStore.StoreEntry?> {
        return container.map {
            it.credentials.firstOrNull { entry ->
                when (entry) {
                    is SubjectCredentialStore.StoreEntry.Vc -> {
                        entry.vc.jwtId == id
                    }

                    is SubjectCredentialStore.StoreEntry.SdJwt -> {
                        entry.sdJwt.jwtId == id
                    }

                    is SubjectCredentialStore.StoreEntry.Iso -> {
                        TODO()
                    }

                    else -> false
                }
            }
        }
    }

    fun observeStoreEntries(): Flow<List<SubjectCredentialStore.StoreEntry>> {
        return container.map { it.credentials }
    }

    fun observeCredentialSize(): Flow<Int> {
        return observeStoreEntries().map { it.size }
    }

    private fun dataStoreValueToStoreContainer(input: String?): StoreContainer {
        if (input == null) {
            return StoreContainer(credentials = mutableListOf(), attachments = mutableListOf())
        } else {
            val export: ExportableStoreContainer = jsonSerializer.decodeFromString(input)
            val credentials = mutableListOf<SubjectCredentialStore.StoreEntry>()
            val attachments = mutableListOf<SubjectCredentialStore.AttachmentEntry>()

            export.credentials.forEach {
                val scheme: ConstantIndex.CredentialScheme
                when (it) {
                    is ExportableStoreEntry.Iso -> {
                        scheme = when (it.scheme) {
                            ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                            ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                            ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            ExportableCredentialScheme.EuPidScheme -> EuPidScheme
                        }
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.Iso(
                                it.issuerSigned,
                                scheme
                            )
                        )
                    }

                    is ExportableStoreEntry.SdJwt -> {
                        scheme = when (it.scheme) {
                            ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                            ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                            ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            ExportableCredentialScheme.EuPidScheme -> EuPidScheme
                        }
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.SdJwt(
                                vcSerialized = it.vcSerialized,
                                sdJwt = it.sdJwt,
                                disclosures = it.disclosures,
                                scheme = scheme
                            )
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        scheme = when (it.scheme) {
                            ExportableCredentialScheme.AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
                            ExportableCredentialScheme.MobileDrivingLicence2023 -> ConstantIndex.MobileDrivingLicence2023
                            ExportableCredentialScheme.IdAustriaScheme -> IdAustriaScheme
                            ExportableCredentialScheme.EuPidScheme -> EuPidScheme
                        }
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.Vc(
                                vcSerialized = it.vcSerialized,
                                vc = it.vc,
                                scheme = scheme
                            )
                        )
                    }
                }
            }
            export.attachments.forEach {
                attachments.add(
                    SubjectCredentialStore.AttachmentEntry(
                        name = it.name,
                        data = it.data,
                        vcId = it.vcId
                    )
                )
            }
            return StoreContainer(credentials, attachments)
        }
    }

    fun observeStoreContainer(): Flow<StoreContainer> {
        return dataStore.getPreference(Configuration.DATASTORE_KEY_VCS).map {
            dataStoreValueToStoreContainer(it)
        }
    }

    fun observeVcs(): Flow<ArrayList<VerifiableCredential>> {
        return this.observeStoreContainer().map {
            val credentialList = ArrayList<VerifiableCredential>()
            it.credentials.forEach { entry ->
                when (entry) {
                    is SubjectCredentialStore.StoreEntry.Iso -> TODO()
                    is SubjectCredentialStore.StoreEntry.Vc -> {
                        credentialList.add(entry.vc.vc)
                    }

                    is SubjectCredentialStore.StoreEntry.SdJwt -> TODO()
                }
            }
            credentialList
        }
    }
}

fun List<SubjectCredentialStore.StoreEntry>.filterNotScheme(scheme: ConstantIndex.CredentialScheme): List<SubjectCredentialStore.StoreEntry> {
    // consider credentials unique by credential scheme
    return this.filterNot {
        when (it) {
            is SubjectCredentialStore.StoreEntry.Vc -> it.scheme == scheme
            is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme == scheme
            is SubjectCredentialStore.StoreEntry.Iso -> it.scheme == scheme
            else -> false
        }
    }
}

fun List<SubjectCredentialStore.StoreEntry>.filterOnlyScheme(scheme: ConstantIndex.CredentialScheme): List<SubjectCredentialStore.StoreEntry> {
    // consider credentials unique by credential scheme
    return this.filter {
        when (it) {
            is SubjectCredentialStore.StoreEntry.Vc -> it.scheme == scheme
            is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme == scheme
            is SubjectCredentialStore.StoreEntry.Iso -> it.scheme == scheme
            else -> false
        }
    }
}

fun StoreContainer.filterNotScheme(scheme: ConstantIndex.CredentialScheme): StoreContainer {
    val credentialIdsWithScheme = credentials.filterOnlyScheme(scheme).mapNotNull { credential ->
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> credential.vc.jwtId
            is SubjectCredentialStore.StoreEntry.SdJwt -> credential.sdJwt.jwtId
            // not sure how to get id for mdoc credentials
            // is SubjectCredentialStore.StoreEntry.Iso -> credential.
            else -> null
        }
    }

    return copy(
        // consider credentials unique by credential scheme
        credentials = credentials.filterNotScheme(scheme),
        // also filter attachments to removed credentials
        attachments = attachments.filterNot { attachment ->
            credentialIdsWithScheme.contains(attachment.vcId)
        },
    )
}

data class StoreContainer(
    val credentials: List<SubjectCredentialStore.StoreEntry>,
    val attachments: List<SubjectCredentialStore.AttachmentEntry>
)

@Serializable
data class ExportableStoreContainer(
    val credentials: MutableList<ExportableStoreEntry>,
    val attachments: MutableList<ExportableAttachmentEntry>
)

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
        return vcId == other.vcId
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + vcId.hashCode()
        return result
    }
}

enum class ExportableCredentialScheme {
    AtomicAttribute2023,
    IdAustriaScheme,
    MobileDrivingLicence2023,
    EuPidScheme,
}

