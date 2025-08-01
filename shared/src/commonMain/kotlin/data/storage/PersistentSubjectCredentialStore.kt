package data.storage

import at.asitplus.KmmResult
import at.asitplus.iso.IssuerSigned
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.random.Random

class PersistentSubjectCredentialStore(
    private val dataStore: DataStoreService
) : SubjectCredentialStore, WalletSubjectCredentialStore {
    private val container = this.observeStoreContainer()

    private suspend fun addStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry) {
        val newContainer = container.first().let {
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
        scheme.schemaUri,
    ).also {
        addStoreEntry(it)
    }


    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: ConstantIndex.CredentialScheme,
    ) = SubjectCredentialStore.StoreEntry.SdJwt(
        vcSerialized,
        vc,
        disclosures,
        scheme.schemaUri,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme,
    ) = SubjectCredentialStore.StoreEntry.Iso(
        issuerSigned,
        scheme.schemaUri,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<ConstantIndex.CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val latestCredentials = container.first().credentials.map { it.second }
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
                        exportableCredentialScheme = storeEntry.scheme!!.toExportableCredentialScheme(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    ExportableStoreEntry.SdJwt(
                        vcSerialized = storeEntry.vcSerialized,
                        sdJwt = storeEntry.sdJwt,
                        disclosures = storeEntry.disclosures,
                        exportableCredentialScheme = storeEntry.scheme!!.toExportableCredentialScheme(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    ExportableStoreEntry.Vc(
                        vcSerialized = storeEntry.vcSerialized,
                        vc = storeEntry.vc,
                        exportableCredentialScheme = storeEntry.scheme!!.toExportableCredentialScheme(),
                    )
                }
            }
        }

        val exportableContainer = ExportableStoreContainer(exportableCredentials)

        val json = vckJsonSerializer.encodeToString(exportableContainer)
        dataStore.setPreference(key = Configuration.DATASTORE_KEY_VCS, value = json)
    }

    override suspend fun reset() {
        exportToDataStore(StoreContainer(credentials = listOf()))
    }

    override suspend fun removeStoreEntryById(storeEntryId: StoreEntryId) {
        val newContainer = container.first().let { latestContainer ->
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
            val export: ExportableStoreContainer = kotlin.runCatching {
                vckJsonSerializer.decodeFromString<ExportableStoreContainer>(input)
            }.getOrElse {
                Napier.w("dataStoreValueToContainer failed for new format", it)
                kotlin.runCatching {
                    ExportableStoreContainer(
                        vckJsonSerializer.decodeFromString<OldExportableStoreContainer>(input).credentials.mapIndexed { index, it ->
                            index.toLong() to it
                        }
                    )
                }.getOrElse {
                    Napier.w("dataStoreValueToContainer failed for old format", it)
                    ExportableStoreContainer(listOf())
                }
            }
            val credentials = export.credentials.map {
                val storeEntryId = it.first
                val storeEntry = it.second
                storeEntryId to when (storeEntry) {
                    is ExportableStoreEntry.Iso -> {
                        SubjectCredentialStore.StoreEntry.Iso(
                            storeEntry.issuerSigned,
                            storeEntry.exportableCredentialScheme.toScheme().schemaUri,
                        )
                    }

                    is ExportableStoreEntry.SdJwt -> {
                        SubjectCredentialStore.StoreEntry.SdJwt(
                            storeEntry.vcSerialized,
                            storeEntry.sdJwt,
                            storeEntry.disclosures,
                            storeEntry.exportableCredentialScheme.toScheme().schemaUri,
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        SubjectCredentialStore.StoreEntry.Vc(
                            storeEntry.vcSerialized,
                            storeEntry.vc,
                            storeEntry.exportableCredentialScheme.toScheme().schemaUri,
                        )
                    }
                }
            }
            return StoreContainer(credentials)
        }
    }

    override fun observeStoreContainer(): Flow<StoreContainer> {
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

/**
 * Used prior to 4.1.0 of the app
 */
@Serializable
private data class OldExportableStoreContainer(
    val credentials: List<ExportableStoreEntry>,
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

enum class ExportableCredentialScheme {
    AtomicAttribute2023, IdAustriaScheme, MobileDrivingLicence2023, EuPidScheme, EuPidSdJwtScheme, PowerOfRepresentationScheme, CertificateOfResidenceScheme, CompanyRegistrationScheme, HealthIdScheme, EhicScheme, TaxIdScheme, VcFallbackCredentialScheme, SdJwtFallbackCredentialScheme, IsoMdocFallbackCredentialScheme;

    @Suppress("DEPRECATION")
    fun toScheme() = when (this) {
        AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
        MobileDrivingLicence2023 -> MobileDrivingLicenceScheme
        IdAustriaScheme -> at.asitplus.wallet.idaustria.IdAustriaScheme
        EuPidScheme -> at.asitplus.wallet.eupid.EuPidScheme
        EuPidSdJwtScheme -> at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
        PowerOfRepresentationScheme -> at.asitplus.wallet.por.PowerOfRepresentationScheme
        CertificateOfResidenceScheme -> at.asitplus.wallet.cor.CertificateOfResidenceScheme
        CompanyRegistrationScheme -> at.asitplus.wallet.companyregistration.CompanyRegistrationScheme
        HealthIdScheme -> at.asitplus.wallet.healthid.HealthIdScheme
        EhicScheme -> at.asitplus.wallet.ehic.EhicScheme
        TaxIdScheme -> at.asitplus.wallet.taxid.TaxIdScheme
        VcFallbackCredentialScheme -> at.asitplus.wallet.lib.data.VcFallbackCredentialScheme
        SdJwtFallbackCredentialScheme -> at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
        IsoMdocFallbackCredentialScheme -> at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
    }

    companion object {
        @Suppress("DEPRECATION")
        fun ConstantIndex.CredentialScheme.toExportableCredentialScheme() = when (this) {
            ConstantIndex.AtomicAttribute2023 -> AtomicAttribute2023
            MobileDrivingLicenceScheme -> MobileDrivingLicence2023
            at.asitplus.wallet.idaustria.IdAustriaScheme -> IdAustriaScheme
            at.asitplus.wallet.eupid.EuPidScheme -> EuPidScheme
            at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme -> EuPidSdJwtScheme
            at.asitplus.wallet.por.PowerOfRepresentationScheme -> PowerOfRepresentationScheme
            at.asitplus.wallet.cor.CertificateOfResidenceScheme -> CertificateOfResidenceScheme
            at.asitplus.wallet.companyregistration.CompanyRegistrationScheme -> CompanyRegistrationScheme
            at.asitplus.wallet.healthid.HealthIdScheme -> HealthIdScheme
            at.asitplus.wallet.ehic.EhicScheme -> EhicScheme
            at.asitplus.wallet.taxid.TaxIdScheme -> TaxIdScheme
            is at.asitplus.wallet.lib.data.VcFallbackCredentialScheme -> VcFallbackCredentialScheme
            is at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme -> SdJwtFallbackCredentialScheme
            is at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme -> IsoMdocFallbackCredentialScheme
            else -> throw Exception("Unknown CredentialScheme")
        }
    }
}
