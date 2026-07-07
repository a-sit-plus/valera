package data.storage

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.iso.IssuerSigned
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtCredentialScheme
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VcJwtCredentialScheme
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatusValidationResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
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
        renewalInfo: CredentialRenewalInfo?,
        issuer: X509Certificate?
    ) = SubjectCredentialStore.StoreEntry.Vc(
        vcSerialized,
        vc,
        renewalInfo = renewalInfo,
        schemeIdentifier = scheme.identifier,
    ).also {
        addStoreEntry(it)
    }


    override suspend fun storeCredential(
        vc: VerifiableCredentialSdJwt,
        vcSerialized: String,
        disclosures: Map<String, SelectiveDisclosureItem?>,
        scheme: SdJwtCredentialScheme,
        renewalInfo: CredentialRenewalInfo?,
        issuer: X509Certificate?
    ) = SubjectCredentialStore.StoreEntry.SdJwt(
        vcSerialized,
        vc,
        disclosures,
        renewalInfo = renewalInfo,
        schemeIdentifier = scheme.identifier,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: IsoMdocCredentialScheme,
        renewalInfo: CredentialRenewalInfo?,
        issuer: X509Certificate?
    ) = SubjectCredentialStore.StoreEntry.Iso(
        issuerSigned,
        renewalInfo = renewalInfo,
        schemeIdentifier = scheme.identifier,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> {
        val latestCredentials = container.first().credentials.map { it.second }
        return credentialSchemes?.let { schemes ->
            KmmResult.success(latestCredentials.filter {
                it.resolveScheme() in schemes
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
                        renewalInfo = storeEntry.renewalInfo,
                        schemeIdentifier = storeEntry.schemeIdentifier,
                    )
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    ExportableStoreEntry.SdJwt(
                        vcSerialized = storeEntry.vcSerialized,
                        sdJwt = storeEntry.sdJwt,
                        disclosures = storeEntry.disclosures,
                        renewalInfo = storeEntry.renewalInfo,
                        schemeIdentifier = storeEntry.schemeIdentifier,
                    )
                }

                is SubjectCredentialStore.StoreEntry.Vc -> {
                    ExportableStoreEntry.Vc(
                        vcSerialized = storeEntry.vcSerialized,
                        vc = storeEntry.vc,
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
    val renewalInfo: CredentialRenewalInfo?
    // has been added nullable to not break de-serializing existing store entries
    val schemeIdentifier: String?
    @Serializable
    data class Vc(
        val vcSerialized: String,
        val vc: VerifiableCredentialJws,
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
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
    ) : ExportableStoreEntry

    @Serializable
    data class Iso(
        val issuerSigned: IssuerSigned,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
    ) : ExportableStoreEntry
}
