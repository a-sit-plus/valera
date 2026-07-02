package data.document

import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import data.credentials.SelectableCredentialSchemes
import data.credentials.mandatoryClaimPaths
import data.credentials.resolveClaimPath
import data.credentials.toIsoElementIdentifier

object RequestDocumentBuilder {
    private val mdlScheme: CredentialScheme get() = SelectableCredentialSchemes.mdl
    private val euPidScheme: CredentialScheme get() = SelectableCredentialSchemes.euPid
    private val ageVerificationScheme: CredentialScheme get() = SelectableCredentialSchemes.ageVerification

    val requestTypeToScheme: Map<SelectableRequestType, CredentialScheme>
        get() = mapOf(
            SelectableRequestType.MDL_MANDATORY to mdlScheme,
            SelectableRequestType.MDL_FULL to mdlScheme,
            SelectableRequestType.MDL_AGE_VERIFICATION to mdlScheme,
            SelectableRequestType.PID_MANDATORY to euPidScheme,
            SelectableRequestType.PID_FULL to euPidScheme,
            SelectableRequestType.AGE_VERIFICATION to ageVerificationScheme,
        )

    fun buildRequestDocument(
        scheme: CredentialScheme,
        subSet: Collection<OpenId4VciClaimsPathPointer>? = null,
    ): RequestDocument {
        val attributes = subSet ?: scheme.claimDescriptions.map { it.path }
        val namespace = scheme.isoNamespace!!
        return RequestDocument(
            docType = scheme.isoDocType!!,
            itemsToRequest = mapOf(namespace to attributes.mapNotNull { it.toIsoElementIdentifier(namespace) }
                .associateWith { false }),
        )
    }

    fun buildRequestDocument(selectableRequest: SelectableRequest) = when (selectableRequest.type) {
        SelectableRequestType.MDL_MANDATORY -> buildRequestDocument(mdlScheme, mdlScheme.mandatoryClaimPaths())
        SelectableRequestType.MDL_FULL -> buildRequestDocument(mdlScheme)
        SelectableRequestType.MDL_AGE_VERIFICATION -> buildRequestDocument(
            mdlScheme,
            listOf(mdlScheme.resolveClaimPath(SelectableAge.fromValue(selectableRequest.age!!)!!.mdlElement))
        )
        SelectableRequestType.PID_MANDATORY -> buildRequestDocument(euPidScheme, euPidScheme.mandatoryClaimPaths())
        SelectableRequestType.PID_FULL -> buildRequestDocument(euPidScheme)
        SelectableRequestType.AGE_VERIFICATION -> buildRequestDocument(
            ageVerificationScheme,
            listOf(ageVerificationScheme.resolveClaimPath(SelectableAge.fromValue(selectableRequest.age!!)!!.avElement))
        )
    }
}

enum class SelectableAge(
    val value: Int,
    val mdlElement: String,
    val avElement: String
) {
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

enum class SelectableRequestType {
    MDL_MANDATORY,
    MDL_FULL,
    MDL_AGE_VERIFICATION,
    PID_MANDATORY,
    PID_FULL,
    AGE_VERIFICATION
}

data class SelectableRequest(
    val type: SelectableRequestType,
    val age: Int? = null
)
