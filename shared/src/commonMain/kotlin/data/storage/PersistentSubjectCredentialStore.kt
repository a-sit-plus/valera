package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.random.Random

class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) :
    SubjectCredentialStore {
    private val container = this.observeStoreContainer()

    private suspend fun addStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry) {
        val newContainer = container.last().let {
            it.copy(it.credentials + listOf(Random.nextLong() to storeEntry))
        }
        exportToDataStore(newContainer)
    }

    override suspend fun storeCredential(
        vc: VerifiableCredentialJws,
        vcSerialized: String,
        scheme: ConstantIndex.CredentialScheme,
    ) = SubjectCredentialStore.StoreEntry.Vc(
        vcSerialized,
        vc,
        scheme,
    ).also {
        addStoreEntry(it)
    }


    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme
    ) = SubjectCredentialStore.StoreEntry.SdJwt(
        vcSerialized,
        vc,
        disclosures,
        scheme,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned, scheme: ConstantIndex.CredentialScheme
    ) = SubjectCredentialStore.StoreEntry.Iso(issuerSigned, scheme).also {
        addStoreEntry(it)
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<ConstantIndex.CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val latestCredentials = container.last().credentials.map { it.second }
        return credentialSchemes?.let { schemes ->
            KmmResult.success(latestCredentials.filter {
                when (it) {
                    is SubjectCredentialStore.StoreEntry.Iso -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme in schemes
                    is SubjectCredentialStore.StoreEntry.Vc -> it.scheme in schemes
                }
            }.toList())
        } ?: KmmResult.success(latestCredentials)
    }

    private suspend fun exportToDataStore(newContainer: StoreContainer) {
        val exportableCredentials = newContainer.credentials.map {
            val storeEntry = it.second
            it.first to when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    ExportableStoreEntry.Iso(
                        issuerSigned = storeEntry.issuerSigned,
                        exportableCredentialScheme = storeEntry.scheme.toExportableCredentialScheme()
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    ExportableStoreEntry.SdJwt(
                        vcSerialized = storeEntry.vcSerialized,
                        sdJwt = storeEntry.sdJwt,
                        disclosures = storeEntry.disclosures,
                        exportableCredentialScheme = storeEntry.scheme.toExportableCredentialScheme()
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    ExportableStoreEntry.Vc(
                        vcSerialized = storeEntry.vcSerialized,
                        vc = storeEntry.vc,
                        exportableCredentialScheme = storeEntry.scheme.toExportableCredentialScheme(),
                    )
                }
            }
        }

        val exportableContainer = ExportableStoreContainer(exportableCredentials)

        val json = vckJsonSerializer.encodeToString(exportableContainer)
        dataStore.setPreference(key = Configuration.DATASTORE_KEY_VCS, value = json)
    }

    suspend fun reset() {
        exportToDataStore(StoreContainer(credentials = listOf()))
    }

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId) {
        val newContainer = container.last().let { latestContainer ->
            latestContainer.copy(
                credentials = latestContainer.credentials.filter {
                    it.first != storeEntryId
                },
            )
        }
        exportToDataStore(newContainer)
    }

    private fun dataStoreValueToStoreContainer(input: String?): StoreContainer {
        if (input == null) {
            return StoreContainer(credentials = mutableListOf())
        } else {
            val export: ExportableStoreContainer = vckJsonSerializer.decodeFromString(input)
            val credentials = export.credentials.map {
                val storeEntryId = it.first
                val storeEntry = it.second
                when (storeEntry) {
                    is ExportableStoreEntry.Iso -> {
                        SubjectCredentialStore.StoreEntry.Iso(
                            storeEntry.issuerSigned,
                            storeEntry.exportableCredentialScheme.toScheme(),
                        )
                    }

                    is ExportableStoreEntry.SdJwt -> {
                        SubjectCredentialStore.StoreEntry.SdJwt(
                            vcSerialized = storeEntry.vcSerialized,
                            sdJwt = storeEntry.sdJwt,
                            disclosures = storeEntry.disclosures,
                            scheme = storeEntry.exportableCredentialScheme.toScheme()
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        SubjectCredentialStore.StoreEntry.Vc(
                            vcSerialized = storeEntry.vcSerialized,
                            vc = storeEntry.vc,
                            scheme = storeEntry.exportableCredentialScheme.toScheme()
                        )
                    }
                }
            }
            return StoreContainer(
                credentials.map {
                    Random.nextLong() to it
                },
            )
        }
    }

    fun observeStoreContainer(): Flow<StoreContainer> {
        return dataStore.getPreference(Configuration.DATASTORE_KEY_VCS).map {
            dataStoreValueToStoreContainer(it)
        }
    }
}

typealias StoreEntryId = Long

@Serializable
data class StoreContainer(
    val credentials: List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>>,
)

@Serializable
private data class ExportableStoreContainer(
    val credentials: List<Pair<StoreEntryId, ExportableStoreEntry>>,
)

@Serializable
private sealed interface ExportableStoreEntry {
    val exportableCredentialScheme: ExportableCredentialScheme

    @Serializable
    data class Vc(
        val vcSerialized: String,
        val vc: VerifiableCredentialJws,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry

    @Serializable
    data class SdJwt(
        val vcSerialized: String,
        val sdJwt: VerifiableCredentialSdJwt,
        /**
         * Map of original serialized disclosure item to parsed item
         */
        val disclosures: Map<String, SelectiveDisclosureItem?>,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry

    @Serializable
    data class Iso(
        val issuerSigned: IssuerSigned,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry
}

private enum class ExportableCredentialScheme {
    AtomicAttribute2023, IdAustriaScheme, MobileDrivingLicence2023, EuPidScheme, PowerOfRepresentationScheme, CertificateOfResidenceScheme;

    fun toScheme() = when (this) {
        AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
        MobileDrivingLicence2023 -> MobileDrivingLicenceScheme
        IdAustriaScheme -> at.asitplus.wallet.idaustria.IdAustriaScheme
        EuPidScheme -> at.asitplus.wallet.eupid.EuPidScheme
        PowerOfRepresentationScheme -> at.asitplus.wallet.por.PowerOfRepresentationScheme
        CertificateOfResidenceScheme -> at.asitplus.wallet.cor.CertificateOfResidenceScheme
    }

    companion object {
        fun ConstantIndex.CredentialScheme.toExportableCredentialScheme() = when (this) {
            ConstantIndex.AtomicAttribute2023 -> AtomicAttribute2023
            MobileDrivingLicenceScheme -> MobileDrivingLicence2023
            at.asitplus.wallet.idaustria.IdAustriaScheme -> IdAustriaScheme
            at.asitplus.wallet.eupid.EuPidScheme -> EuPidScheme
            at.asitplus.wallet.por.PowerOfRepresentationScheme -> PowerOfRepresentationScheme
            at.asitplus.wallet.cor.CertificateOfResidenceScheme -> CertificateOfResidenceScheme
            else -> throw Exception("Unknown CredentialScheme")
        }
    }
}
