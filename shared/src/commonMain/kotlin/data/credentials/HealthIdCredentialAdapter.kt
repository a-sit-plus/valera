package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.healthid.HealthIdScheme.Attributes
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class HealthIdCredentialAdapter : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                Attributes.HEALTH_INSURANCE_ID -> Attribute.fromValue(healthInsuranceId)
                Attributes.PATIENT_ID -> Attribute.fromValue(patientId)
                Attributes.TAX_NUMBER -> Attribute.fromValue(taxNumber)
                Attributes.ONE_TIME_TOKEN -> Attribute.fromValue(oneTimeToken)
                Attributes.E_PRESCRIPTION_CODE -> Attribute.fromValue(ePrescriptionCode)
                Attributes.AFFILIATION_COUNTRY -> Attribute.fromValue(affiliationCountry)
                Attributes.ISSUE_DATE -> Attribute.fromValue(issueDate)
                Attributes.EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                Attributes.ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                Attributes.DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                Attributes.ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                Attributes.ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                Attributes.ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)
                else -> null
            }

            else -> null
        }
    }

    abstract val healthInsuranceId: String?
    abstract val patientId: String?
    abstract val taxNumber: String?
    abstract val oneTimeToken: String?
    abstract val ePrescriptionCode: String?
    abstract val affiliationCountry: String?
    abstract val issueDate: Instant?
    abstract val expiryDate: Instant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val administrativeNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): HealthIdCredentialAdapter {
            if (storeEntry.scheme !is HealthIdScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> TODO("Operation not yet supported")
                is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { HealthIdComplexCredentialSdJwtAdapter(it) }
                    ?: HealthIdCredentialSdJwtAdapter(storeEntry.toAttributeMap())

                is SubjectCredentialStore.StoreEntry.Iso -> HealthIdCredentialIsoMdocAdapter(storeEntry.toNamespaceAttributeMap())
            }
        }
    }
}

private class HealthIdCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : HealthIdCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = HealthIdScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val healthInsuranceId: String?
        get() = attributes[Attributes.HEALTH_INSURANCE_ID]?.contentOrNull

    override val patientId: String?
        get() = attributes[Attributes.PATIENT_ID]?.contentOrNull

    override val taxNumber: String?
        get() = attributes[Attributes.TAX_NUMBER]?.contentOrNull

    override val oneTimeToken: String?
        get() = attributes[Attributes.ONE_TIME_TOKEN]?.contentOrNull

    override val ePrescriptionCode: String?
        get() = attributes[Attributes.E_PRESCRIPTION_CODE]?.contentOrNull

    override val affiliationCountry: String?
        get() = attributes[Attributes.AFFILIATION_COUNTRY]?.contentOrNull

    override val issueDate: Instant?
        get() = attributes[Attributes.ISSUE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[Attributes.EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = attributes[Attributes.ISSUING_AUTHORITY]?.contentOrNull

    override val documentNumber: String?
        get() = attributes[Attributes.DOCUMENT_NUMBER]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[Attributes.ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuingCountry: String?
        get() = attributes[Attributes.ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[Attributes.ISSUING_JURISDICTION]?.contentOrNull
}

private class HealthIdComplexCredentialSdJwtAdapter(
    private val attributes: JsonObject,
) : HealthIdCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = HealthIdScheme

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val healthInsuranceId: String?
        get() = (attributes[Attributes.HEALTH_INSURANCE_ID] as? JsonPrimitive?)?.contentOrNull

    override val patientId: String?
        get() = (attributes[Attributes.PATIENT_ID] as? JsonPrimitive?)?.contentOrNull

    override val taxNumber: String?
        get() = (attributes[Attributes.TAX_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val oneTimeToken: String?
        get() = (attributes[Attributes.ONE_TIME_TOKEN] as? JsonPrimitive?)?.contentOrNull

    override val ePrescriptionCode: String?
        get() = (attributes[Attributes.E_PRESCRIPTION_CODE] as? JsonPrimitive?)?.contentOrNull

    override val affiliationCountry: String?
        get() = (attributes[Attributes.AFFILIATION_COUNTRY] as? JsonPrimitive?)?.contentOrNull

    override val issueDate: Instant?
        get() = (attributes[Attributes.ISSUE_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = (attributes[Attributes.EXPIRY_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = (attributes[Attributes.ISSUING_AUTHORITY] as? JsonPrimitive?)?.contentOrNull

    override val documentNumber: String?
        get() = (attributes[Attributes.DOCUMENT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val administrativeNumber: String?
        get() = (attributes[Attributes.ADMINISTRATIVE_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuingCountry: String?
        get() = (attributes[Attributes.ISSUING_COUNTRY] as? JsonPrimitive?)?.contentOrNull

    override val issuingJurisdiction: String?
        get() = (attributes[Attributes.ISSUING_JURISDICTION] as? JsonPrimitive?)?.contentOrNull
}

class HealthIdCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : HealthIdCredentialAdapter() {
    override val scheme: ConstantIndex.CredentialScheme
        get() = HealthIdScheme

    private val namespace = namespaces?.get(HealthIdScheme.isoNamespace)

    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.ISO_MDOC

    override val healthInsuranceId: String?
        get() = namespace?.get(Attributes.HEALTH_INSURANCE_ID) as String?

    override val patientId: String?
        get() = namespace?.get(Attributes.PATIENT_ID) as String?

    override val taxNumber: String?
        get() = namespace?.get(Attributes.TAX_NUMBER) as String?

    override val oneTimeToken: String?
        get() = namespace?.get(Attributes.ONE_TIME_TOKEN) as String?

    override val ePrescriptionCode: String?
        get() = namespace?.get(Attributes.E_PRESCRIPTION_CODE) as String?

    override val affiliationCountry: String?
        get() = namespace?.get(Attributes.AFFILIATION_COUNTRY) as String?

    override val issueDate: Instant?
        get() = namespace?.get(Attributes.ISSUE_DATE)?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = namespace?.get(Attributes.EXPIRY_DATE)?.toInstantOrNull()

    override val issuingAuthority: String?
        get() = namespace?.get(Attributes.ISSUING_AUTHORITY) as String?

    override val documentNumber: String?
        get() = namespace?.get(Attributes.DOCUMENT_NUMBER) as String?

    override val administrativeNumber: String?
        get() = namespace?.get(Attributes.ADMINISTRATIVE_NUMBER) as String?

    override val issuingCountry: String?
        get() = namespace?.get(Attributes.ISSUING_COUNTRY) as String?

    override val issuingJurisdiction: String?
        get() = namespace?.get(Attributes.ISSUING_JURISDICTION) as String?

}
