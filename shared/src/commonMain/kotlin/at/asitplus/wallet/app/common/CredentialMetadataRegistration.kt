package at.asitplus.wallet.app.common

import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EU_PID_METADATA_URL
import at.asitplus.wallet.eupid.EuPidItemValueSerializerMap
import at.asitplus.wallet.eupid.EuPidJsonValueEncoder
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_METADATA_URL
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.CredentialMetadataLookup
import at.asitplus.wallet.lib.ktor.openid.RemoteCredentialMetadataRegistry
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import at.asitplus.wallet.mdl.MDL_METADATA_URL
import at.asitplus.wallet.mdl.MobileDrivingLicenceItemValueSerializerMap
import at.asitplus.wallet.mdl.MobileDrivingLicenceJsonValueEncoder
import at.asitplus.wallet.sdjwt.SdJwtVcType
import data.storage.DataStoreService
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Raw type-metadata documents for credentials not bundled in vck core (credentials-collection, main branch). */
private const val CREDENTIALS_COLLECTION_BASE =
    "https://raw.githubusercontent.com/a-sit-plus/credentials-collection/main/"

/** `urn:eudi:ehic:1` */
const val EHIC_VCT = "urn:eudi:ehic:1"

/** `urn:eu.europa.ec.eudi:tax:1` */
const val TAX_ID_VCT = "urn:eu.europa.ec.eudi:tax:1"

/** `eu.europa.ec.eudi.cor.1` */
const val COR_VCT = "eu.europa.ec.eudi.cor.1"

/** `urn:eu.europa.ec.eudi:cr:1` */
const val COMPANY_REGISTRATION_VCT = "urn:eu.europa.ec.eudi:cr:1"

/** `urn:eu.europa.ec.eudi:por:1` */
const val POR_VCT = "urn:eu.europa.ec.eudi:por:1"

/** `urn:eu.europa.ec.eudi:hiid:1` */
const val HEALTH_ID_VCT = "urn:eu.europa.ec.eudi:hiid:1"

/** `eu.europa.ec.av.1` */
const val AV_DOC_TYPE = "eu.europa.ec.av.1"

/** vct -> hosted document URL for the remotely-resolved credentials. */
private val remoteCredentialDocumentUrls: Map<SdJwtVcType, String> = mapOf(
    SdJwtVcType("EuPid2023") to EU_PID_METADATA_URL,
    SdJwtVcType(EU_PID_SD_JWT_VCT) to EU_PID_SD_JWT_METADATA_URL,
    SdJwtVcType(MDL_DOCTYPE) to MDL_METADATA_URL,
    SdJwtVcType(EHIC_VCT) to "${CREDENTIALS_COLLECTION_BASE}ehic.json",
    SdJwtVcType(TAX_ID_VCT) to "${CREDENTIALS_COLLECTION_BASE}tax-id-credential.json",
    SdJwtVcType(COR_VCT) to "${CREDENTIALS_COLLECTION_BASE}certificate-of-residence.json",
    SdJwtVcType(COMPANY_REGISTRATION_VCT) to "${CREDENTIALS_COLLECTION_BASE}company-registration.json",
    SdJwtVcType(POR_VCT) to "${CREDENTIALS_COLLECTION_BASE}power-of-representation.json",
    SdJwtVcType(HEALTH_ID_VCT) to "${CREDENTIALS_COLLECTION_BASE}healthid.json",
    SdJwtVcType(AV_DOC_TYPE) to "${CREDENTIALS_COLLECTION_BASE}age-verification.json",
)

private val credentialMetadataRegistrationLock = SynchronizedObject()
private var credentialMetadataRegistered = false

fun registerCredentialMetadata(
    buildContext: BuildContext,
    dataStoreService: DataStoreService,
    httpService: HttpService? = null,
) {
    synchronized(credentialMetadataRegistrationLock) {
        if (credentialMetadataRegistered) return
        credentialMetadataRegistered = true
    }
    registerBundledCredentialMetadata()
    registerRemoteCredentialMetadata(
        dataStoreService,
        httpService ?: HttpService(buildContext),
    )
}

/** EU PID, EU PID SD-JWT and mDL serializers ship bundled in vck core. Metadata labels come from remote documents. */
private fun registerBundledCredentialMetadata() {
    LibraryInitializer.registerCredentialSerializers(
        jsonValueEncoder = MobileDrivingLicenceJsonValueEncoder,
        itemValueSerializerMap = MobileDrivingLicenceItemValueSerializerMap,
    )
    LibraryInitializer.registerCredentialSerializers(
        jsonValueEncoder = EuPidJsonValueEncoder,
        itemValueSerializerMap = EuPidItemValueSerializerMap,
    )
}

private fun registerRemoteCredentialMetadata(dataStoreService: DataStoreService, httpService: HttpService) {
    val remote = RemoteCredentialMetadataRegistry(
        httpClient = httpService.cachedResourceClient(dataStoreService, revalidate = true),
        clock = Clock.System,
        documentUrls = remoteCredentialDocumentUrls.toMutableMap(),
        // ISO mdoc lookups use docType identifiers; alias them to the vct keys used by the remote documents.
        aliases = mapOf(
            CredentialMetadataLookup(ISO_MDOC, EU_PID_DOCTYPE) to SdJwtVcType("EuPid2023"),
            CredentialMetadataLookup(ISO_MDOC, MDL_DOCTYPE) to SdJwtVcType(MDL_DOCTYPE),
            CredentialMetadataLookup(ISO_MDOC, AV_DOC_TYPE) to SdJwtVcType(AV_DOC_TYPE),
        ),
    )
    // Persist resolved metadata so each scheme is fetched at most once per cache period, also across restarts.
    LibraryInitializer.registerCredentialMetadataRegistry(
        PersistentCachingCredentialMetadataRegistry(
            delegate = remote,
            dataStore = dataStoreService,
            ttl = Configuration.CACHE_TTL_CREDENTIAL_METADATA,
        )
    )
}
