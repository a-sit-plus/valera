package data.storage

import at.asitplus.catchingUnwrapped
import at.asitplus.csc.serializers.Base64X509CertificateSerializer
import at.asitplus.iso.IssuerSigned
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.identifier
import at.asitplus.wallet.lib.agent.CredentialRenewalInfo
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.data.IsoMdocCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtCredentialScheme
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VcJwtCredentialScheme
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Serializing the credential container is CPU-intensive (in particular for ISO credentials whose
 * issuer-signed items contain byte arrays and nested CBOR). Keep it away from UI dispatchers and
 * serialize only one container at a time per process so multiple wallet sessions cannot saturate
 * all available cores with duplicate work.
 */
private val credentialStoreSerializationDispatcher = Dispatchers.Default

class PersistentSubjectCredentialStore(
    private val dataStore: DataStoreService,
    override val validator: Validator,
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
        vcSerialized = vcSerialized,
        vc = vc,
        renewalInfo = renewalInfo,
        issuer = issuer,
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
        vcSerialized = vcSerialized,
        sdJwt = vc,
        disclosures = disclosures,
        renewalInfo = renewalInfo,
        issuer = issuer,
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
        issuerSigned = issuerSigned,
        renewalInfo = renewalInfo,
        issuer = issuer,
        schemeIdentifier = scheme.identifier,
    ).also {
        addStoreEntry(it)
    }

    private suspend fun exportToDataStore(newContainer: StoreContainer) {
        val json = withContext(credentialStoreSerializationDispatcher) {
            val exportableCredentials = newContainer.credentials.map {
                val storeEntry = it.second
                it.first to when (storeEntry) {
                    is SubjectCredentialStore.StoreEntry.Iso -> {
                        ExportableStoreEntry.Iso(
                            issuerSigned = storeEntry.issuerSigned,
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                        issuer = storeEntry.issuer)
                    }

                    is SubjectCredentialStore.StoreEntry.SdJwt -> {
                        ExportableStoreEntry.SdJwt(
                            vcSerialized = storeEntry.vcSerialized,
                            sdJwt = storeEntry.sdJwt,
                            disclosures = storeEntry.disclosures,
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                        issuer = storeEntry.issuer)
                    }

                    is SubjectCredentialStore.StoreEntry.Vc -> {
                        ExportableStoreEntry.Vc(
                            vcSerialized = storeEntry.vcSerialized,
                            vc = storeEntry.vc,
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                        issuer = storeEntry.issuer)
                    }
                }
            }

            joseCompliantSerializer.encodeToString(ExportableStoreContainer(exportableCredentials))
        }
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
                            issuer = storeEntry.issuer
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
                            issuer = storeEntry.issuer
                        )
                    }

                    is ExportableStoreEntry.Vc -> {
                        SubjectCredentialStore.StoreEntry.Vc(
                            vcSerialized = storeEntry.vcSerialized,
                            vc = storeEntry.vc,
                            schemaUri = "not relevant",
                            renewalInfo = storeEntry.renewalInfo,
                            schemeIdentifier = storeEntry.schemeIdentifier,
                            issuer = storeEntry.issuer
                        )
                    }
                }
            }
            return StoreContainer(credentials)
        }
    }

    override fun observeStoreContainer(): Flow<StoreContainer> {
        return dataStore.getPreference(Configuration.DATASTORE_KEY_VCS).map {
            withContext(credentialStoreSerializationDispatcher) {
                dataStoreValueToStoreContainer(it)
            }
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
    val renewalInfo: CredentialRenewalInfo?
    // has been added nullable to not break de-serializing existing store entries
    val schemeIdentifier: String?
    val issuer: X509Certificate?
    @Serializable
    data class Vc(
        val vcSerialized: String,
        val vc: VerifiableCredentialJws,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
        @Serializable(with = Base64X509CertificateSerializer::class)
        override val issuer: X509Certificate? = null
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
        @Serializable(with = Base64X509CertificateSerializer::class)
        override val issuer: X509Certificate? = null
    ) : ExportableStoreEntry

    @Serializable
    data class Iso(
        val issuerSigned: IssuerSigned,
        override val renewalInfo: CredentialRenewalInfo? = null,
        override val schemeIdentifier: String? = null,
        @Serializable(with = Base64X509CertificateSerializer::class)
        override val issuer: X509Certificate? = null
    ) : ExportableStoreEntry
}
