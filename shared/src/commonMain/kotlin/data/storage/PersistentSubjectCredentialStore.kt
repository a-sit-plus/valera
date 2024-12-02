package data.storage

import at.asitplus.KmmResult
import at.asitplus.signum.indispensable.cosef.CoseHeader
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.wallet.app.common.Configuration
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.SelectiveDisclosureItem
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.iso.IssuerSigned
import at.asitplus.wallet.lib.iso.IssuerSignedList
import at.asitplus.wallet.lib.iso.NamespacedIssuerSignedListSerializer
import at.asitplus.wallet.lib.iso.vckCborSerializer
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.storage.ExportableCredentialScheme.Companion.toExportableCredentialScheme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.ByteString
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlin.random.Random

class PersistentSubjectCredentialStore(private val dataStore: DataStoreService) :
    SubjectCredentialStore {
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
        scheme,
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
        scheme,
    ).also {
        addStoreEntry(it)
    }

    override suspend fun storeCredential(
        issuerSigned: IssuerSigned,
        scheme: ConstantIndex.CredentialScheme,
    ) = SubjectCredentialStore.StoreEntry.Iso(issuerSigned, scheme).also {
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
                    ExportableStoreEntry.IsoNew(
                        issuerSignedSerialized = storeEntry.issuerSigned.serialize(),
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
            }.getOrElse { ex ->
                Napier.w("Could not load ExportableStoreContainer", ex)
                ExportableStoreContainer(
                    vckJsonSerializer.decodeFromString<OldExportableStoreContainer>(input).credentials
                        .mapIndexed { index, it -> index.toLong() to it }
                )
            }
            val credentials = export.credentials.map {
                val storeEntryId = it.first
                val storeEntry = it.second
                storeEntryId to when (storeEntry) {
                    is ExportableStoreEntry.Iso -> {
                        SubjectCredentialStore.StoreEntry.Iso(
                            IssuerSigned.deserialize(storeEntry.issuerSigned.serialize()).getOrThrow(),
                            storeEntry.exportableCredentialScheme.toScheme(),
                        )
                    }

                    is ExportableStoreEntry.IsoNew -> SubjectCredentialStore.StoreEntry.Iso(
                        storeEntry.issuerSigned,
                        storeEntry.exportableCredentialScheme.toScheme(),
                    )

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
            return StoreContainer(credentials)
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

    @Deprecated(replaceWith = ReplaceWith("IsoNew"), message = "Use other data class")
    @Serializable
    data class Iso(
        val issuerSigned: IssuerSignedWallet,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry

    @Serializable
    data class IsoNew(
        val issuerSignedSerialized: ByteArray,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry {
        val issuerSigned: IssuerSigned by lazy {
            IssuerSigned.deserialize(issuerSignedSerialized).getOrThrow()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as IsoNew

            if (!issuerSignedSerialized.contentEquals(other.issuerSignedSerialized)) return false
            if (exportableCredentialScheme != other.exportableCredentialScheme) return false

            return true
        }

        override fun hashCode(): Int {
            var result = issuerSignedSerialized.contentHashCode()
            result = 31 * result + exportableCredentialScheme.hashCode()
            return result
        }
    }
}

/**
 * Workaround to deserialize stored entries prior to 5.4.0, then serialize it encoded
 */
@Serializable
private data class IssuerSignedWallet constructor(
    @SerialName("nameSpaces")
    @Serializable(with = NamespacedIssuerSignedListSerializer::class)
    val namespaces: Map<String, @Contextual IssuerSignedList>? = null,
    @SerialName("issuerAuth")
    val issuerAuth: CoseSignedWallet,
) {
    fun serialize() = vckCborSerializer.encodeToByteArray(this)
}

/**
 * Workaround to deserialize stored entries prior to 5.4.0, then serialize it encoded
 */
@Serializable
@CborArray
private data class CoseSignedWallet(
    @ByteString
    val protectedHeader: ByteStringWrapper<CoseHeader>,
    val unprotectedHeader: CoseHeader?,
    @ByteString
    val payload: ByteArray?,
    @ByteString
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CoseSignedWallet

        if (protectedHeader != other.protectedHeader) return false
        if (unprotectedHeader != other.unprotectedHeader) return false
        if (payload != null) {
            if (other.payload == null) return false
            if (!payload.contentEquals(other.payload)) return false
        } else if (other.payload != null) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = protectedHeader.hashCode()
        result = 31 * result + (unprotectedHeader?.hashCode() ?: 0)
        result = 31 * result + (payload?.contentHashCode() ?: 0)
        result = 31 * result + signature.contentHashCode()
        return result
    }
}


enum class ExportableCredentialScheme {
    AtomicAttribute2023, IdAustriaScheme, MobileDrivingLicence2023, EuPidScheme, PowerOfRepresentationScheme, CertificateOfResidenceScheme, EPrescriptionScheme;

    fun toScheme() = when (this) {
        AtomicAttribute2023 -> ConstantIndex.AtomicAttribute2023
        MobileDrivingLicence2023 -> MobileDrivingLicenceScheme
        IdAustriaScheme -> at.asitplus.wallet.idaustria.IdAustriaScheme
        EuPidScheme -> at.asitplus.wallet.eupid.EuPidScheme
        PowerOfRepresentationScheme -> at.asitplus.wallet.por.PowerOfRepresentationScheme
        CertificateOfResidenceScheme -> at.asitplus.wallet.cor.CertificateOfResidenceScheme
        EPrescriptionScheme -> at.asitplus.wallet.eprescription.EPrescriptionScheme
    }

    companion object {
        fun ConstantIndex.CredentialScheme.toExportableCredentialScheme() = when (this) {
            ConstantIndex.AtomicAttribute2023 -> AtomicAttribute2023
            MobileDrivingLicenceScheme -> MobileDrivingLicence2023
            at.asitplus.wallet.idaustria.IdAustriaScheme -> IdAustriaScheme
            at.asitplus.wallet.eupid.EuPidScheme -> EuPidScheme
            at.asitplus.wallet.por.PowerOfRepresentationScheme -> PowerOfRepresentationScheme
            at.asitplus.wallet.cor.CertificateOfResidenceScheme -> CertificateOfResidenceScheme
            at.asitplus.wallet.eprescription.EPrescriptionScheme -> EPrescriptionScheme
            else -> throw Exception("Unknown CredentialScheme")
        }
    }
}
