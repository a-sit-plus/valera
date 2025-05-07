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

    fun itemsToRequestDocument(docType: String, namespace: String, entries: Set<String>) =
        RequestDocument(
            docType = docType,
            itemsToRequest = mapOf(namespace to entries.associateWith { false })
        )

    fun getMdlMandatoryAttributesRequestDocument() = RequestDocument(
        docType = MobileDrivingLicenceScheme.isoDocType,
        itemsToRequest = mapOf(
            MobileDrivingLicenceScheme.isoNamespace to
                    MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.associateWith { false }
        )
    )

    fun getMdlFullAttributesRequestDocument() = RequestDocument(
        docType = MobileDrivingLicenceScheme.isoDocType,
        itemsToRequest = mapOf(
            MobileDrivingLicenceScheme.isoNamespace to
                    MobileDrivingLicenceDataElements.ALL_ELEMENTS.associateWith { false }
        )
    )

    fun getPidRequiredAttributesRequestDocument() = RequestDocument(
        docType = EuPidScheme.isoDocType,
        itemsToRequest = mapOf(
            EuPidScheme.isoNamespace to EuPidScheme.requiredClaimNames.associateWith { false }
        )
    )

    fun getPidFullAttributesRequestDocument() = RequestDocument(
        docType = EuPidScheme.isoDocType,
        itemsToRequest = mapOf(
            EuPidScheme.isoNamespace to EuPidScheme.claimNames.associateWith { false }
        )
    )

    fun getAgeVerificationRequestDocument(age: Int): RequestDocument {
        val elementName = when (age) {
            SelectableAges.OVER_12 -> MobileDrivingLicenceDataElements.AGE_OVER_12
            SelectableAges.OVER_14 -> MobileDrivingLicenceDataElements.AGE_OVER_14
            SelectableAges.OVER_16 -> MobileDrivingLicenceDataElements.AGE_OVER_16
            SelectableAges.OVER_18 -> MobileDrivingLicenceDataElements.AGE_OVER_18
            SelectableAges.OVER_21 -> MobileDrivingLicenceDataElements.AGE_OVER_21
            else -> MobileDrivingLicenceDataElements.AGE_OVER_18
        }
        return RequestDocument(
            docType = MobileDrivingLicenceScheme.isoDocType,
            itemsToRequest = mapOf(
                MobileDrivingLicenceScheme.isoNamespace to mapOf(elementName to false)
            )
        )
    }
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

// At the moment only used for mDL
object SelectableAges {
    const val OVER_12 = 12
    const val OVER_14 = 14
    const val OVER_16 = 16
    const val OVER_18 = 18
    const val OVER_21 = 21

    val values = listOf(OVER_12, OVER_14, OVER_16, OVER_18, OVER_21)
}
