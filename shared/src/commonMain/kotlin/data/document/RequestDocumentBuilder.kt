package data.document

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.AV_DOC_TYPE
import at.asitplus.wallet.app.common.HEALTH_ID_VCT
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EuPidDataElements
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import data.credentials.metadataLabel

// Age Verification (ISO mdoc) and Health ID (SD-JWT) are resolved from remote type metadata; their
// claim names are kept here as constants for the verifier request presets, instead of compiled-in scheme objects.

private val ageVerificationElements = listOf(
    "age_over_12", "age_over_13", "age_over_14", "age_over_16", "age_over_18",
    "age_over_21", "age_over_25", "age_over_60", "age_over_62", "age_over_65", "age_over_68",
)
private val healthIdElements = listOf(
    "issue_date", "expiry_date", "issuing_authority", "issuing_country", "health_insurance_id",
    "patient_id", "tax_number", "one_time_token", "wallet_e_prescription_code", "affiliation_country",
    "document_number", "administrative_number", "issuing_jurisdiction",
)

object RequestDocumentBuilder {

    // EU PID, mDL are resolved from type metadata (no compiled-in scheme objects); AgeVerification and
    // HealthId still ship as libraries. Everything is keyed on the ISO docType.
    @Suppress("DEPRECATION")
    private val euPidMandatoryElements = with(EuPidDataElements) {
        listOf(FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, NATIONALITY, EXPIRY_DATE, ISSUING_AUTHORITY, ISSUING_COUNTRY)
    }

    @Suppress("DEPRECATION")
    private val euPidAllElements = with(EuPidDataElements) {
        listOf(
            FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, FAMILY_NAME_BIRTH, GIVEN_NAME_BIRTH, PLACE_OF_BIRTH,
            RESIDENT_ADDRESS, RESIDENT_COUNTRY, RESIDENT_STATE, RESIDENT_CITY, RESIDENT_POSTAL_CODE,
            RESIDENT_STREET, RESIDENT_HOUSE_NUMBER, SEX, NATIONALITY, ISSUANCE_DATE, EXPIRY_DATE,
            ISSUING_AUTHORITY, DOCUMENT_NUMBER, ISSUING_COUNTRY, ISSUING_JURISDICTION,
            PERSONAL_ADMINISTRATIVE_NUMBER, PORTRAIT, EMAIL_ADDRESS, MOBILE_PHONE_NUMBER, TRUST_ANCHOR,
            LOCATION_STATUS,
        )
    }

    /** Resolve a metadata-backed scheme by ISO docType, falling back to an unknown scheme (namespace == docType). */
    private fun schemeFor(docType: String): CredentialScheme =
        AttributeIndex.resolveIsoDoctype(docType) ?: IsoMdocFallbackCredentialScheme(isoDocType = docType)

    private val mdlScheme: CredentialScheme get() = schemeFor(MDL_DOCTYPE)
    private val euPidScheme: CredentialScheme get() = schemeFor(EU_PID_DOCTYPE)
    private val ageVerificationScheme: CredentialScheme get() = schemeFor(AV_DOC_TYPE)
    private val healthIdScheme: CredentialScheme get() = schemeFor(HEALTH_ID_VCT)

    val schemes: List<CredentialScheme>
        get() = listOf(mdlScheme, euPidScheme, healthIdScheme, ageVerificationScheme)

    val requestTypeToScheme: Map<SelectableRequestType, CredentialScheme>
        get() = mapOf(
            SelectableRequestType.MDL_MANDATORY to mdlScheme,
            SelectableRequestType.MDL_FULL to mdlScheme,
            SelectableRequestType.MDL_AGE_VERIFICATION to mdlScheme,
            SelectableRequestType.PID_MANDATORY to euPidScheme,
            SelectableRequestType.PID_FULL to euPidScheme,
            SelectableRequestType.AGE_VERIFICATION to ageVerificationScheme,
            SelectableRequestType.HIID to healthIdScheme,
        )

    private val preselectionByDocType: Map<String, () -> Set<String>> = mapOf(
        MDL_DOCTYPE to { MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.toSet() },
        EU_PID_DOCTYPE to { euPidMandatoryElements.toSet() },
        HEALTH_ID_VCT to { healthIdRequiredElements.toSet() },
        AV_DOC_TYPE to { setOf("age_over_18") },
    )

    private val allElementsByDocType: Map<String, List<String>> = mapOf(
        MDL_DOCTYPE to MobileDrivingLicenceDataElements.ALL_ELEMENTS,
        EU_PID_DOCTYPE to euPidAllElements,
        HEALTH_ID_VCT to healthIdElements,
        AV_DOC_TYPE to ageVerificationElements,
    )

    private val docTypeConfigs: Map<String, DocTypeConfig>
        get() = schemes.associate { scheme ->
            val docType = scheme.isoDocType!!
            docType to DocTypeConfig(
                scheme = scheme,
                preselection = preselectionByDocType[docType] ?: { emptySet() },
                translator = { path -> scheme.metadataLabel(path) },
            )
        }

    fun getDocTypeConfig(docType: String): DocTypeConfig? = docTypeConfigs[docType]

    fun getPreselection(docType: String): Set<String> =
        docTypeConfigs[docType]?.preselection?.invoke() ?: emptySet()

    fun buildRequestDocument(
        scheme: CredentialScheme,
        subSet: Collection<String>? = null,
    ): RequestDocument {
        val attributes = subSet ?: allElementsByDocType[scheme.isoDocType] ?: emptyList()
        return RequestDocument(
            docType = scheme.isoDocType!!,
            itemsToRequest = mapOf(scheme.isoNamespace!! to attributes.associateWith { false }),
        )
    }

    fun buildRequestDocument(selectableRequest: SelectableRequest) = when (selectableRequest.type) {
        SelectableRequestType.MDL_MANDATORY -> buildRequestDocument(
            mdlScheme, MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS
        )
        SelectableRequestType.MDL_FULL -> buildRequestDocument(mdlScheme)
        SelectableRequestType.MDL_AGE_VERIFICATION -> buildRequestDocument(
            mdlScheme, listOf(SelectableAge.fromValue(selectableRequest.age!!)!!.mdlElement!!)
        )
        SelectableRequestType.PID_MANDATORY -> buildRequestDocument(euPidScheme, euPidMandatoryElements)
        SelectableRequestType.PID_FULL -> buildRequestDocument(euPidScheme)
        SelectableRequestType.AGE_VERIFICATION -> buildRequestDocument(
            ageVerificationScheme, listOf(SelectableAge.fromValue(selectableRequest.age!!)!!.avElement!!)
        )
        SelectableRequestType.HIID -> buildRequestDocument(
            healthIdScheme, healthIdRequiredElements
        )
    }
}

object SelectableDocTypes {
    val docTypes: Set<String> = RequestDocumentBuilder.schemes.mapNotNull { it.isoDocType }.toSet()
}

data class DocTypeConfig(
    val scheme: CredentialScheme,
    val preselection: () -> Set<String>,
    val translator: (NormalizedJsonPath) -> String?
)

enum class SelectableAge(val value: Int, val mdlElement: String?, val avElement: String?) {
    OVER_12(12, MobileDrivingLicenceDataElements.AGE_OVER_12, "age_over_12"),
    OVER_13(13, MobileDrivingLicenceDataElements.AGE_OVER_13, "age_over_13"),
    OVER_14(14, MobileDrivingLicenceDataElements.AGE_OVER_14, "age_over_14"),
    OVER_16(16, MobileDrivingLicenceDataElements.AGE_OVER_16, "age_over_16"),
    OVER_18(18, MobileDrivingLicenceDataElements.AGE_OVER_18, "age_over_18"),
    OVER_21(21, MobileDrivingLicenceDataElements.AGE_OVER_21, "age_over_21"),
    OVER_25(25, MobileDrivingLicenceDataElements.AGE_OVER_25, "age_over_25"),
    OVER_60(60, MobileDrivingLicenceDataElements.AGE_OVER_60, "age_over_60"),
    OVER_62(62, MobileDrivingLicenceDataElements.AGE_OVER_62, "age_over_62"),
    OVER_65(65, MobileDrivingLicenceDataElements.AGE_OVER_65, "age_over_65"),
    OVER_68(68, MobileDrivingLicenceDataElements.AGE_OVER_68, "age_over_68");

    companion object {
        val valuesList = entries.map { it.value }
        fun fromValue(value: Int) = entries.find { it.value == value }
    }
}

/** Required Health ID claims for the verifier preset (Health ID is resolved from remote type metadata). */
private val healthIdRequiredElements: List<String> = listOf(
    "one_time_token", "affiliation_country", "issue_date", "expiry_date", "issuing_authority", "issuing_country",
)

enum class SelectableRequestType {
    MDL_MANDATORY,
    MDL_FULL,
    MDL_AGE_VERIFICATION,
    PID_MANDATORY,
    PID_FULL,
    AGE_VERIFICATION,
    HIID
}

data class SelectableRequest(
    val type: SelectableRequestType,
    val age: Int? = null
)
