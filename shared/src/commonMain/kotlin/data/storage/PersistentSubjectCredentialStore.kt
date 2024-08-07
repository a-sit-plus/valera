package data.storage

import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
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
        val exportableCredentials = mutableListOf<ExportableStoreEntry>()
        newContainer.credentials.forEach {
            when (it) {
                is SubjectCredentialStore.StoreEntry.Iso -> {
                    exportableCredentials.add(
                        ExportableStoreEntry.Iso(
                            issuerSigned = it.issuerSigned,
                            exportableCredentialScheme = it.scheme.toExportableCredentialScheme()
                        )
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    exportableCredentials.add(
                        ExportableStoreEntry.SdJwt(
                            vcSerialized = it.vcSerialized,
                            sdJwt = it.sdJwt,
                            disclosures = it.disclosures,
                            exportableCredentialScheme = it.scheme.toExportableCredentialScheme()
                        )
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    exportableCredentials.add(
                        ExportableStoreEntry.Vc(
                            vcSerialized = it.vcSerialized,
                            vc = it.vc,
                            exportableCredentialScheme = it.scheme.toExportableCredentialScheme(),
                        )
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
            val export: ExportableStoreContainer = vckJsonSerializer.decodeFromString(input)
            val credentials = mutableListOf<SubjectCredentialStore.StoreEntry>()

            export.credentials.forEach {
                val scheme: ConstantIndex.CredentialScheme
                when (it) {
                    is ExportableStoreEntry.Iso -> {
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.Iso(
                                it.issuerSigned,
                                it.exportableCredentialScheme.toScheme()
                            )
                        )
                    }

                    is ExportableStoreEntry.SdJwt -> {
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.SdJwt(
                                vcSerialized = it.vcSerialized,
                                sdJwt = it.sdJwt,
                                disclosures = it.disclosures,
                                scheme = it.exportableCredentialScheme.toScheme()
                            )
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        credentials.add(
                            SubjectCredentialStore.StoreEntry.Vc(
                                vcSerialized = it.vcSerialized, vc = it.vc, scheme = it.exportableCredentialScheme.toScheme()
                            )
                        )
                    }
                }
            }
            return StoreContainer(credentials)
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

@Serializable
private data class ExportableStoreContainer(
    val credentials: MutableList<ExportableStoreEntry>,
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
    AtomicAttribute2023,
    IdAustriaScheme,
    MobileDrivingLicence2023,
    EuPidScheme,
    PowerOfRepresentationScheme,
    CertificateOfResidenceScheme;

    fun toScheme() = when(this) {
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
