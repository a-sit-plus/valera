package data.document

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.HealthIdCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import org.jetbrains.compose.resources.StringResource

object RequestDocumentBuilder {
    val schemes: List<CredentialScheme> = listOf(
        MobileDrivingLicenceScheme,
        EuPidScheme,
        HealthIdScheme
    )

    val requestTypeToScheme: Map<SelectableRequestType, CredentialScheme> = mapOf(
        SelectableRequestType.MDL_MANDATORY to MobileDrivingLicenceScheme,
        SelectableRequestType.MDL_FULL to MobileDrivingLicenceScheme,
        SelectableRequestType.MDL_AGE_VERIFICATION to MobileDrivingLicenceScheme,
        SelectableRequestType.PID_MANDATORY to EuPidScheme,
        SelectableRequestType.PID_FULL to EuPidScheme,
        SelectableRequestType.PID_AGE_VERIFICATION to EuPidScheme,
        SelectableRequestType.HIID to HealthIdScheme
    )

    private val translatorMapping = mapOf(
        MobileDrivingLicenceScheme::class to MobileDrivingLicenceCredentialAttributeTranslator::translate,
        EuPidScheme::class to EuPidCredentialAttributeTranslator::translate,
        HealthIdScheme::class to HealthIdCredentialAttributeTranslator::translate
    )

    private val preselectionMapping = mapOf(
        MobileDrivingLicenceScheme::class to { MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.toSet() },
        EuPidScheme::class to { EuPidScheme.requiredClaimNames.toSet() },
        HealthIdScheme::class to { HealthIdSchemeRequiredClaimNames.attributes.toSet() }
    )

    private val docTypeConfigs: Map<String, DocTypeConfig> = schemes.associate { scheme ->
        val translator = translatorMapping[scheme::class] ?: { _: NormalizedJsonPath -> null }
        val preselection = preselectionMapping[scheme::class] ?: { scheme.claimNames.toSet() }
        scheme.isoDocType!! to DocTypeConfig(
            scheme = scheme,
            preselection = preselection,
            translator = translator
        )
    }

    fun getDocTypeConfig(docType: String): DocTypeConfig? = docTypeConfigs[docType]

    fun getPreselection(docType: String): Set<String> =
        docTypeConfigs[docType]?.preselection?.invoke() ?: emptySet()

    fun buildRequestDocument(
        scheme: CredentialScheme,
        subSet: Collection<String>? = null
    ): RequestDocument {
        val attributes = subSet ?: scheme.claimNames
        return RequestDocument(
            docType = scheme.isoDocType!!,
            itemsToRequest = mapOf(scheme.isoNamespace!! to attributes.associateWith { false })
        )
    }

    fun buildRequestDocument(selectableRequest: SelectableRequest) = when (selectableRequest.type) {
        SelectableRequestType.MDL_MANDATORY -> buildRequestDocument(
            MobileDrivingLicenceScheme, MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS
        )
        SelectableRequestType.MDL_FULL -> buildRequestDocument(MobileDrivingLicenceScheme)
        SelectableRequestType.MDL_AGE_VERIFICATION -> buildRequestDocument(
            MobileDrivingLicenceScheme, listOf(
                SelectableAge.fromValue(selectableRequest.age!!)!!.mdlElement!!
            )
        )
        SelectableRequestType.PID_MANDATORY -> buildRequestDocument(
            EuPidScheme, EuPidScheme.requiredClaimNames
        )
        SelectableRequestType.PID_FULL -> buildRequestDocument(EuPidScheme)
        SelectableRequestType.PID_AGE_VERIFICATION -> buildRequestDocument(
            EuPidScheme, listOf(
                SelectableAge.fromValue(selectableRequest.age!!)!!.pidElement!!
            )
        )
        SelectableRequestType.HIID -> buildRequestDocument(
            HealthIdScheme, HealthIdSchemeRequiredClaimNames.attributes
        )
    }
}

object SelectableDocTypes {
    val docTypes: Set<String> = RequestDocumentBuilder.schemes.mapNotNull { it.isoDocType }.toSet()
}

data class DocTypeConfig(
    val scheme: CredentialScheme,
    val preselection: () -> Set<String>,
    val translator: (NormalizedJsonPath) -> StringResource?
)

enum class SelectableAge(val value: Int, val mdlElement: String?, val pidElement: String?) {
    OVER_12(12, MobileDrivingLicenceDataElements.AGE_OVER_12, EuPidScheme.Attributes.AGE_OVER_12),
    OVER_13(13, MobileDrivingLicenceDataElements.AGE_OVER_13, EuPidScheme.Attributes.AGE_OVER_13),
    OVER_14(14, MobileDrivingLicenceDataElements.AGE_OVER_14, EuPidScheme.Attributes.AGE_OVER_14),
    OVER_16(16, MobileDrivingLicenceDataElements.AGE_OVER_16, EuPidScheme.Attributes.AGE_OVER_16),
    OVER_18(18, MobileDrivingLicenceDataElements.AGE_OVER_18, EuPidScheme.Attributes.AGE_OVER_18),
    OVER_21(21, MobileDrivingLicenceDataElements.AGE_OVER_21, EuPidScheme.Attributes.AGE_OVER_21),
    OVER_25(25, MobileDrivingLicenceDataElements.AGE_OVER_25, EuPidScheme.Attributes.AGE_OVER_25),
    OVER_60(60, MobileDrivingLicenceDataElements.AGE_OVER_60, EuPidScheme.Attributes.AGE_OVER_60),
    OVER_62(62, MobileDrivingLicenceDataElements.AGE_OVER_62, EuPidScheme.Attributes.AGE_OVER_62),
    OVER_65(65, MobileDrivingLicenceDataElements.AGE_OVER_65, EuPidScheme.Attributes.AGE_OVER_65),
    OVER_68(68, MobileDrivingLicenceDataElements.AGE_OVER_68, EuPidScheme.Attributes.AGE_OVER_68);

    companion object {
        val valuesList = entries.map { it.value }
        fun fromValue(value: Int) = entries.find { it.value == value }
    }
}

object HealthIdSchemeRequiredClaimNames {
    val attributes: List<String> = listOf(
        HealthIdScheme.Attributes.ONE_TIME_TOKEN,
        HealthIdScheme.Attributes.AFFILIATION_COUNTRY,
        HealthIdScheme.Attributes.ISSUE_DATE,
        HealthIdScheme.Attributes.EXPIRY_DATE,
        HealthIdScheme.Attributes.ISSUING_AUTHORITY,
        HealthIdScheme.Attributes.ISSUING_COUNTRY
    )
}

enum class SelectableRequestType {
    MDL_MANDATORY,
    MDL_FULL,
    MDL_AGE_VERIFICATION,
    PID_MANDATORY,
    PID_FULL,
    PID_AGE_VERIFICATION,
    HIID
}

data class SelectableRequest(
    val type: SelectableRequestType,
    val age: Int? = null
)
