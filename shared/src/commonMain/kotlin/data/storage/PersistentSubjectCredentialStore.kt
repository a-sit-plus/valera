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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
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
        container.first().let {
            it.copy(credentials = it.credentials + listOf(Random.nextLong() to storeEntry))
        }.exportToDataStore()
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
        scheme.schemaUri
    ).also {
        addStoreEntry(it)
    }

    override suspend fun getCredentials(
        credentialSchemes: Collection<ConstantIndex.CredentialScheme>?,
    ): KmmResult<List<SubjectCredentialStore.StoreEntry>> =
        container.first().credentials.map { it.second }.let { latestCredentials ->
            credentialSchemes?.let { schemes ->
                KmmResult.success(latestCredentials.filter {
                    when (it) {
                        is SubjectCredentialStore.StoreEntry.Iso -> it.scheme in schemes
                        is SubjectCredentialStore.StoreEntry.SdJwt -> it.scheme in schemes
                        is SubjectCredentialStore.StoreEntry.Vc -> it.scheme in schemes
                    }
                }.toList())
            } ?: KmmResult.success(latestCredentials)
        }

    private suspend fun StoreContainer.exportToDataStore() {
        Napier.i("Storing StoreContainer with ${this.credentials.size} entries")
        dataStore.setPreference(
            key = Configuration.DATASTORE_KEY_VCS,
            value = vckJsonSerializer.encodeToString<StoreContainer>(this)
        )
    }

    suspend fun reset() {
        StoreContainer(credentials = listOf()).exportToDataStore()
    }

    suspend fun removeStoreEntryById(storeEntryId: StoreEntryId) {
        container.first().let { latestContainer ->
            latestContainer.copy(
                credentials = latestContainer.credentials.filter {
                    it.first != storeEntryId
                },
            )
        }.exportToDataStore()
    }

    private fun String.dataStoreValueToStoreContainer(): StoreContainer =
        kotlin.runCatching {
            vckJsonSerializer.decodeFromString<StoreContainer>(this)
                .also { Napier.i("Loaded StoreContainer with ${it.credentials.size} entries") }
        }.getOrElse { ex1 ->
            Napier.w("Could not load StoreContainer", ex1)
            Napier.i("Trying ExportableStoreContainer (old)")
            kotlin.runCatching {
                @Suppress("DEPRECATION")
                vckJsonSerializer.decodeFromString<ExportableStoreContainer>(this).toStoreContainer()
                    .also { Napier.i("Loaded ExportableStoreContainer with ${it.credentials.size} entries") }
            }.getOrElse { ex ->
                Napier.w("Could not load ExportableStoreContainer", ex)
                StoreContainer(listOf())
            }
        }

    @Suppress("DEPRECATION")
    private fun ExportableStoreContainer.toStoreContainer(): StoreContainer =
        StoreContainer(credentials.map { (storeEntryId, storeEntry) ->
            storeEntryId to when (storeEntry) {
                is ExportableStoreEntry.Iso -> SubjectCredentialStore.StoreEntry.Iso(
                    IssuerSigned.deserialize(storeEntry.issuerSigned.serialize()).getOrThrow(),
                    storeEntry.exportableCredentialScheme.toScheme().schemaUri,
                )

                is ExportableStoreEntry.SdJwt -> SubjectCredentialStore.StoreEntry.SdJwt(
                    storeEntry.vcSerialized,
                    storeEntry.sdJwt,
                    storeEntry.disclosures,
                    storeEntry.exportableCredentialScheme.toScheme().schemaUri
                )

                is ExportableStoreEntry.Vc -> SubjectCredentialStore.StoreEntry.Vc(
                    storeEntry.vcSerialized,
                    storeEntry.vc,
                    storeEntry.exportableCredentialScheme.toScheme().schemaUri
                )
            }
        })

    fun observeStoreContainer(): Flow<StoreContainer> =
        dataStore.getPreference(Configuration.DATASTORE_KEY_VCS)
            .mapNotNull { it }
            .mapNotNull { it.dataStoreValueToStoreContainer() }
}

typealias StoreEntryId = Long

@Serializable
data class StoreContainer(
    val credentials: List<Pair<StoreEntryId, SubjectCredentialStore.StoreEntry>>,
)

/**
 * Used prior to 5.4.0 of the app
 */
@Suppress("DEPRECATION")
@Deprecated(message = "Use StoreContainer", replaceWith = ReplaceWith("StoreContainer"))
@Serializable
private data class ExportableStoreContainer(
    val credentials: List<Pair<StoreEntryId, ExportableStoreEntry>>,
)

@Suppress("DEPRECATION")
@Deprecated(message = "Use StoreEntry from vc-k")
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
        val issuerSigned: IssuerSignedWallet,
        override val exportableCredentialScheme: ExportableCredentialScheme
    ) : ExportableStoreEntry

}

/**
 * Workaround to deserialize stored entries prior to 5.4.0, then serialize it encoded
 */
@Serializable
private data class IssuerSignedWallet(
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


@Deprecated(message = "Use StoreEntry from vc-k")
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
}
