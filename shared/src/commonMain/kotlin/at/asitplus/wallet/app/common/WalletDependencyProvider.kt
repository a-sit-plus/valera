package at.asitplus.wallet.app.common

import at.asitplus.wallet.eupid.EU_PID_METADATA_URL
import at.asitplus.wallet.eupid.EuPidItemValueSerializerMap
import at.asitplus.wallet.eupid.EuPidJsonValueEncoder
import at.asitplus.wallet.eupid.EuPidMetadataDocument
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_METADATA_URL
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtMetadataDocument
import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.CredentialMetadataLookup
import at.asitplus.wallet.lib.data.StaticCredentialMetadataRegistry
import at.asitplus.wallet.lib.ktor.openid.RemoteCredentialMetadataRegistry
import at.asitplus.wallet.mdl.MDL_METADATA_URL
import at.asitplus.wallet.mdl.MobileDrivingLicenceItemValueSerializerMap
import at.asitplus.wallet.mdl.MobileDrivingLicenceJsonValueEncoder
import at.asitplus.wallet.mdl.MobileDrivingLicenceMetadataDocument
import at.asitplus.wallet.sdjwt.SdJwtTypeMetadataDocumentRegistry
import at.asitplus.wallet.sdjwt.SdJwtVcType
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.multipaz.prompt.PromptModel

/** Raw type-metadata documents for credentials not bundled in vck core (credentials-collection, main branch). */
private const val CREDENTIALS_COLLECTION_BASE =
    "https://raw.githubusercontent.com/a-sit-plus/credentials-collection/main"

/** vct -> hosted document URL for the remotely-resolved credentials. */
private val remoteCredentialDocumentUrls: Map<SdJwtVcType, String> = mapOf(
    SdJwtVcType("urn:eudi:ehic:1") to "${CREDENTIALS_COLLECTION_BASE}ehic.json",
    SdJwtVcType("urn:eu.europa.ec.eudi:tax:1") to "${CREDENTIALS_COLLECTION_BASE}tax-id-credential.json",
    SdJwtVcType("eu.europa.ec.eudi.cor.1") to "${CREDENTIALS_COLLECTION_BASE}certificate-of-residence.json",
    SdJwtVcType("urn:eu.europa.ec.eudi:cr:1") to "${CREDENTIALS_COLLECTION_BASE}company-registration.json",
    SdJwtVcType("urn:eu.europa.ec.eudi:por:1") to "${CREDENTIALS_COLLECTION_BASE}power-of-representation.json",
    SdJwtVcType("urn:eu.europa.ec.eudi:hiid:1") to "${CREDENTIALS_COLLECTION_BASE}healthid.json",
    SdJwtVcType("eu.europa.ec.av.1") to "${CREDENTIALS_COLLECTION_BASE}age-verification.json",
)

@OptIn(ExperimentalTime::class)
private fun registerRemoteCredentialMetadata(buildContext: BuildContext, dataStoreService: DataStoreService) {
    val remote = RemoteCredentialMetadataRegistry(
        httpClient = HttpService(buildContext).buildHttpClient(),
        clock = Clock.System,
        documentUrls = remoteCredentialDocumentUrls.toMutableMap(),
        // age-verification is ISO mdoc: its docType (== vct) must be aliased to the document's vct.
        aliases = mapOf(
            CredentialMetadataLookup(ISO_MDOC, "eu.europa.ec.av.1") to SdJwtVcType("eu.europa.ec.av.1"),
        ),
    )
    // Persist resolved metadata so each scheme is fetched from the network only once (also across restarts).
    LibraryInitializer.registerCredentialMetadataRegistry(
        PersistentCachingCredentialMetadataRegistry(delegate = remote, dataStore = dataStoreService)
    )
}

/** EU PID, EU PID SD-JWT and mDL ship bundled in vck core; register their metadata + serializers. */
private fun registerBundledCredentialMetadata() {
    LibraryInitializer.registerCredentialMetadataRegistry(
        StaticCredentialMetadataRegistry(
            documentRegistry = SdJwtTypeMetadataDocumentRegistry(
                EuPidSdJwtMetadataDocument,
                EuPidMetadataDocument,
                MobileDrivingLicenceMetadataDocument,
            ),
            documentUrls = mapOf(
                EuPidSdJwtMetadataDocument.first to EU_PID_SD_JWT_METADATA_URL,
                EuPidMetadataDocument.first to EU_PID_METADATA_URL,
                MobileDrivingLicenceMetadataDocument.first to MDL_METADATA_URL,
            ),
        )
    )
    LibraryInitializer.registerCredentialSerializers(
        jsonValueEncoder = MobileDrivingLicenceJsonValueEncoder,
        itemValueSerializerMap = MobileDrivingLicenceItemValueSerializerMap,
    )
    LibraryInitializer.registerCredentialSerializers(
        jsonValueEncoder = EuPidJsonValueEncoder,
        itemValueSerializerMap = EuPidItemValueSerializerMap,
    )
}

data class WalletDependencyProvider(
    val keystoreService: KeystoreService,
    val dataStoreService: DataStoreService,
    val platformAdapter: PlatformAdapter,
    var subjectCredentialStore: PersistentSubjectCredentialStore =
        PersistentSubjectCredentialStore(dataStoreService, Validator()),
    val buildContext: BuildContext,
    val promptModel: PromptModel,
    val antilog: Antilog,
) {
    init {
        registerBundledCredentialMetadata()
        registerRemoteCredentialMetadata(buildContext, dataStoreService)

        Napier.takeLogarithm()
        Napier.base(antilog)
    }
}
