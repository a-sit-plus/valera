package data.document

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.HealthIdCredentialAttributeTranslator
import data.credentials.IdAustriaCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

object RequestDocumentBuilder {
    val schemes: List<CredentialScheme> = listOf(
        MobileDrivingLicenceScheme,
        EuPidScheme,
        HealthIdScheme,
        IdAustriaScheme
    )

    private val translatorMapping: Map<KClass<out CredentialScheme>, (NormalizedJsonPath) -> StringResource?> = mapOf(
        MobileDrivingLicenceScheme::class to { path -> MobileDrivingLicenceCredentialAttributeTranslator.translate(path) },
        EuPidScheme::class to { path -> EuPidCredentialAttributeTranslator.translate(path) },
        HealthIdScheme::class to { path -> HealthIdCredentialAttributeTranslator.translate(path) },
        IdAustriaScheme::class to { path -> IdAustriaCredentialAttributeTranslator.translate(path) }
    )

    private val preselectionMapping: Map<KClass<out CredentialScheme>, () -> Set<String>> = mapOf(
        MobileDrivingLicenceScheme::class to { MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS.toSet() },
        EuPidScheme::class to { EuPidScheme.requiredClaimNames.toSet() },
        HealthIdScheme::class to { HealthIdSchemeRequiredClaimNames.getAttributes().toSet() },
        IdAustriaScheme::class to { IdAustriaScheme.claimNames.toSet() }
    )

    val docTypeConfigs: Map<String, DocTypeConfig> = schemes.associate { scheme ->
        val translator = translatorMapping[scheme::class] ?: { _: NormalizedJsonPath -> null }
        val preselection = preselectionMapping[scheme::class] ?: { scheme.claimNames.toSet() }
        scheme.isoDocType!! to DocTypeConfig(
            scheme = scheme,
            claimNames = scheme.claimNames,
            preselection = preselection,
            translator = translator
        )
    }

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

    fun getMdlFullAttributesRequestDocument() = buildRequestDocument(MobileDrivingLicenceScheme)

    fun getMdlMandatoryAttributesRequestDocument() = buildRequestDocument(
        MobileDrivingLicenceScheme, MobileDrivingLicenceDataElements.MANDATORY_ELEMENTS
    )

    fun getAgeVerificationRequestDocumentMdl(age: Int) = buildRequestDocument(
        MobileDrivingLicenceScheme, listOf(SelectableAge.fromValue(age)!!.mdlElement!!)
    )

    fun getPidFullAttributesRequestDocument() = buildRequestDocument(EuPidScheme)

    fun getPidRequiredAttributesRequestDocument() = buildRequestDocument(
        EuPidScheme, EuPidScheme.requiredClaimNames
    )

    fun getAgeVerificationRequestDocumentPid(age: Int) = buildRequestDocument(
        EuPidScheme, listOf(SelectableAge.fromValue(age)!!.pidElement!!)
    )

    fun getHealthIdRequiredAttributesRequestDocument() = buildRequestDocument(
        HealthIdScheme, HealthIdSchemeRequiredClaimNames.getAttributes()
    )
}

object SelectableDocTypes {
    val docTypes: Set<String> = RequestDocumentBuilder.schemes.mapNotNull { it.isoDocType }.toSet()
}

data class DocTypeConfig(
    val scheme: CredentialScheme,
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

object HealthIdSchemeRequiredClaimNames {
    fun getAttributes(): Collection<String> {
        return listOf(
            HealthIdScheme.Attributes.ONE_TIME_TOKEN,
            HealthIdScheme.Attributes.AFFILIATION_COUNTRY,
            HealthIdScheme.Attributes.ISSUE_DATE,
            HealthIdScheme.Attributes.EXPIRY_DATE,
            HealthIdScheme.Attributes.ISSUING_AUTHORITY,
            HealthIdScheme.Attributes.ISSUING_COUNTRY
        )
    }
}
