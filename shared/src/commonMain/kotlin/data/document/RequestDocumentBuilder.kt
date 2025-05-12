package data.document

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import org.jetbrains.compose.resources.StringResource

object RequestDocumentBuilder {
    val docTypeConfigs = mapOf(
        SelectableDocTypes.MDL to DocTypeConfig(
            namespace = MobileDrivingLicenceScheme.isoNamespace,
            docType = MobileDrivingLicenceScheme.isoDocType,
            claimNames = MobileDrivingLicenceScheme.claimNames,
            preselection = { MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.toSet() },
            translator = { path -> MobileDrivingLicenceCredentialAttributeTranslator.translate(path) }
        ),
        SelectableDocTypes.PID to DocTypeConfig(
            namespace = EuPidScheme.isoNamespace,
            docType = EuPidScheme.isoDocType,
            claimNames = EuPidScheme.claimNames,
            preselection = { EuPidScheme.requiredClaimNames.toSet() },
            translator = { path -> EuPidCredentialAttributeTranslator.translate(path) }
        )
    )

    fun getPreselection(docType: String): Set<String> =
        docTypeConfigs[docType]?.preselection?.invoke() ?: emptySet()

    fun buildRequestDocument(
        docType: String,
        namespace: String,
        attributes: Collection<String>
    ) = RequestDocument(
        docType = docType,
        itemsToRequest = mapOf(namespace to attributes.associateWith { false })
    )

    fun getMdlMandatoryAttributesRequestDocument() = buildRequestDocument(
        MobileDrivingLicenceScheme.isoDocType,
        MobileDrivingLicenceScheme.isoNamespace,
        MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS
    )

    fun getMdlFullAttributesRequestDocument() = buildRequestDocument(
        MobileDrivingLicenceScheme.isoDocType,
        MobileDrivingLicenceScheme.isoNamespace,
        MobileDrivingLicenceDataElements.ALL_ELEMENTS
    )

    fun getPidRequiredAttributesRequestDocument() = buildRequestDocument(
        EuPidScheme.isoDocType,
        EuPidScheme.isoNamespace,
        EuPidScheme.requiredClaimNames
    )

    fun getPidFullAttributesRequestDocument() = buildRequestDocument(
        EuPidScheme.isoDocType,
        EuPidScheme.isoNamespace,
        EuPidScheme.claimNames
    )

    fun getAgeVerificationRequestDocumentMdl(age: Int) = buildRequestDocument(
        MobileDrivingLicenceScheme.isoDocType,
        MobileDrivingLicenceScheme.isoNamespace,
        listOf(SelectableAge.fromValue(age)!!.mdlElement!!)
    )

    fun getAgeVerificationRequestDocumentPid(age: Int) = buildRequestDocument(
        EuPidScheme.isoDocType,
        EuPidScheme.isoNamespace,
        listOf(SelectableAge.fromValue(age)!!.pidElement!!)
    )
}

object SelectableDocTypes {
    val MDL = MobileDrivingLicenceScheme.isoDocType
    val PID = EuPidScheme.isoDocType
}

data class DocTypeConfig(
    val namespace: String,
    val docType: String,
    val claimNames: Collection<String>,
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
