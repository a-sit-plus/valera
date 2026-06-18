package data.storage

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.iso.IssuerSigned
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtCredentialScheme
import at.asitplus.wallet.lib.data.VcJwtCredentialScheme
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPidIso
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isEuPidSdJwt
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.isMdl
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.random.Random

class PersistentSubjectCredentialStore(
    private val dataStore: DataStoreService,
    private val validator: Validator
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
        scheme: VcJwtCredentialScheme,
        renewalInfo: CredentialRenewalInfo?
    ) = SubjectCredentialStore.StoreEntry.Vc(
        vcSerialized,
        vc,
        scheme.schemaUri,
        renewalInfo,
    ).also {
        addStoreEntry(it)
    }


    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: SdJwtCredentialScheme,
        renewalInfo: CredentialRenewalInfo?
    ) = SubjectCredentialStore.StoreEntry.SdJwt(
        vcSerialized,
        vc,
        disclosures,
        scheme.schemaUri,
        renewalInfo,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: IsoMdocCredentialScheme,
        renewalInfo: CredentialRenewalInfo?
    ) = SubjectCredentialStore.StoreEntry.Iso(
        issuerSigned,
        scheme.schemaUri,
        renewalInfo,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<CredentialScheme>?,
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
                        exportableCredentialScheme = storeEntry.resolveScheme().toExportableCredentialScheme(),
                        renewalInfo = storeEntry.renewalInfo,
                        schemeIdentifier = storeEntry.schemeIdentifier,
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    ExportableStoreEntry.SdJwt(
                        vcSerialized = storeEntry.vcSerialized,
                        sdJwt = storeEntry.sdJwt,
                        disclosures = storeEntry.disclosures,
                        exportableCredentialScheme = storeEntry.resolveScheme().toExportableCredentialScheme(),
                        renewalInfo = storeEntry.renewalInfo,
                        schemeIdentifier = storeEntry.schemeIdentifier,
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    ExportableStoreEntry.Vc(
                        vcSerialized = storeEntry.vcSerialized,
                        vc = storeEntry.vc,
                        exportableCredentialScheme = storeEntry.resolveScheme().toExportableCredentialScheme(),
                        renewalInfo = storeEntry.renewalInfo,
                        schemeIdentifier = storeEntry.schemeIdentifier,
                    )
                }
            }
        }

        val exportableContainer = ExportableStoreContainer(exportableCredentials)

        val json = joseCompliantSerializer.encodeToString(exportableContainer)
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
            val export: ExportableStoreContainer = catchingUnwrapped {
                joseCompliantSerializer.decodeFromString<ExportableStoreContainer>(input)
            }.getOrElse {
                Napier.w("dataStoreValueToContainer failed for new format", it)
                catchingUnwrapped {
                    ExportableStoreContainer(
                        joseCompliantSerializer.decodeFromString<OldExportableStoreContainer>(input).credentials.mapIndexed { index, it ->
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
                            issuerSigned = storeEntry.issuerSigned,
                            schemaUri = "not relevant",
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                        )
                    }

                    is ExportableStoreEntry.SdJwt -> {
                        SubjectCredentialStore.StoreEntry.SdJwt(
                            vcSerialized = storeEntry.vcSerialized,
                            sdJwt = storeEntry.sdJwt,
                            disclosures = storeEntry.disclosures,
                            schemaUri = "not relevant",
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        SubjectCredentialStore.StoreEntry.Vc(
                            vcSerialized = storeEntry.vcSerialized,
                            vc = storeEntry.vc,
                            schemaUri = "not relevant",
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
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

    /**
     * Checks all stored credentials and returns a list of those that are no longer fresh.
     * Returns a list of Pairs containing the unique StoreEntryId and the Entry itself.
     */
    override suspend fun getInvalidCredentials(): List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>> {
        val availableCredentials = container.first().credentials
        if (availableCredentials.isEmpty()) return emptyList()

        return coroutineScope {
            val deferredStatus = availableCredentials.map { (id, entry) ->
                (id to entry) to async {
                    validator.checkCredentialFreshness(entry)
                }
            }

            deferredStatus.map { (pair, deferred) ->
                pair to deferred.await()
            }.filter { (pair, freshness) ->
                pair.second.renewalInfo != null && freshness.needsRefresh
            }.map { (pair, _) ->
                pair
            }
        }
    }
}

private val CredentialFreshnessSummary.needsRefresh: Boolean
    get() = timelinessValidationSummary.isExpired ||
            tokenStatusValidationResult is TokenStatusValidationResult.Invalid

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
    val renewalInfo: CredentialRenewalInfo?
    // has been added nullable to not break de-serializing existing store entries
    val schemeIdentifier: String?
    @Serializable
    data class Vc(
        val vcSerialized: String,
        val vc: VerifiableCredentialJws,
        override val exportableCredentialScheme: ExportableCredentialScheme,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
    ) : ExportableStoreEntry

    @Serializable
    data class SdJwt(
        val vcSerialized: String,
        val sdJwt: VerifiableCredentialSdJwt,
        /**
         * Map of original serialized disclosure item to parsed item
         */
        val disclosures: Map<String, SelectiveDisclosureItem?>,
        override val exportableCredentialScheme: ExportableCredentialScheme,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
    ) : ExportableStoreEntry

    @Serializable
    data class Iso(
        val issuerSigned: IssuerSigned,
        override val exportableCredentialScheme: ExportableCredentialScheme,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
    ) : ExportableStoreEntry
}

enum class ExportableCredentialScheme {
    AtomicAttribute2023, MobileDrivingLicence2023, EuPidScheme, EuPidSdJwtScheme, PowerOfRepresentationScheme, CertificateOfResidenceScheme, CompanyRegistrationScheme, HealthIdScheme, EhicScheme, TaxIdScheme, VcFallbackCredentialScheme, SdJwtFallbackCredentialScheme, IsoMdocFallbackCredentialScheme, AgeVerificationScheme;

    @Suppress("DEPRECATION")
    fun toScheme() = when (this) {
        AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
        MobileDrivingLicence2023 -> AttributeIndex.resolveIsoDoctype(MDL_DOCTYPE)
            ?: at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme(isoDocType = MDL_DOCTYPE)
        AgeVerificationScheme -> at.asitplus.wallet.ageverification.AgeVerificationScheme
        EuPidScheme -> AttributeIndex.resolveIsoDoctype(EU_PID_DOCTYPE)
            ?: at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme(isoDocType = EU_PID_DOCTYPE)
        EuPidSdJwtScheme -> AttributeIndex.resolveSdJwtAttributeType(EU_PID_SD_JWT_VCT)
            ?: at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme(sdJwtType = EU_PID_SD_JWT_VCT)
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
        fun CredentialScheme.toExportableCredentialScheme() = when {
            this == ConstantIndex.AtomicAttribute2023 -> AtomicAttribute2023
            isMdl -> MobileDrivingLicence2023
            isEuPidIso -> EuPidScheme
            isEuPidSdJwt -> EuPidSdJwtScheme
            this == at.asitplus.wallet.ageverification.AgeVerificationScheme -> AgeVerificationScheme
            this == at.asitplus.wallet.por.PowerOfRepresentationScheme -> PowerOfRepresentationScheme
            this == at.asitplus.wallet.cor.CertificateOfResidenceScheme -> CertificateOfResidenceScheme
            this == at.asitplus.wallet.companyregistration.CompanyRegistrationScheme -> CompanyRegistrationScheme
            this == at.asitplus.wallet.healthid.HealthIdScheme -> HealthIdScheme
            this == at.asitplus.wallet.ehic.EhicScheme -> EhicScheme
            this == at.asitplus.wallet.taxid.TaxIdScheme -> TaxIdScheme
            this is at.asitplus.wallet.lib.data.VcFallbackCredentialScheme -> VcFallbackCredentialScheme
            this is at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme -> SdJwtFallbackCredentialScheme
            this is at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme -> IsoMdocFallbackCredentialScheme
            else -> throw Exception("Unknown CredentialScheme")
        }
    }
}
