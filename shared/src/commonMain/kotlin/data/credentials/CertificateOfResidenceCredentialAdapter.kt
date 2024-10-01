package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ARRIVAL_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.BIRTH_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.BIRTH_PLACE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.DOCUMENT_NUMBER
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.EXPIRY_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.FAMILY_NAME
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.GENDER
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.GIVEN_NAME
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUANCE_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUING_AUTHORITY
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUING_COUNTRY
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ISSUING_JURISDICTION
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.NATIONALITY
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed class CertificateOfResidenceCredentialAdapter : CredentialAdapter() {
    override fun getAttribute(path: NormalizedJsonPath) = path.segments.firstOrNull()?.let { first ->
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                ADMINISTRATIVE_NUMBER -> Attribute.fromValue(administrativeNumber)
                ISSUANCE_DATE -> Attribute.fromValue(issuanceDate)
                EXPIRY_DATE -> Attribute.fromValue(expiryDate)
                ISSUING_AUTHORITY -> Attribute.fromValue(issuingAuthority)
                DOCUMENT_NUMBER -> Attribute.fromValue(documentNumber)
                ISSUING_COUNTRY -> Attribute.fromValue(issuingCountry)
                ISSUING_JURISDICTION -> Attribute.fromValue(issuingJurisdiction)

                FAMILY_NAME -> Attribute.fromValue(familyName)
                GIVEN_NAME -> Attribute.fromValue(givenName)
                BIRTH_DATE -> Attribute.fromValue(birthDate)
                RESIDENCE_ADDRESS -> Attribute.fromValue(residenceAddress)
                GENDER -> Attribute.fromValue(gender)
                BIRTH_PLACE -> Attribute.fromValue(birthPlace)
                ARRIVAL_DATE -> Attribute.fromValue(arrivalDate)
                NATIONALITY -> Attribute.fromValue(nationality)

                else -> null
            }

            else -> null
        }
    }

    abstract val familyName: String?
    abstract val givenName: String?
    abstract val birthDate: LocalDate?
    abstract val residenceAddress: String?
    abstract val gender: IsoIec5218Gender?
    abstract val birthPlace: String?
    abstract val arrivalDate: LocalDate?
    abstract val nationality: String?
    abstract val administrativeNumber: String?
    abstract val issuanceDate: Instant?
    abstract val expiryDate: Instant?
    abstract val issuingAuthority: String?
    abstract val documentNumber: String?
    abstract val issuingCountry: String?
    abstract val issuingJurisdiction: String?

    companion object {
        fun createFromStoreEntry(storeEntry: SubjectCredentialStore.StoreEntry): CertificateOfResidenceCredentialAdapter {
            if (storeEntry.scheme !is CertificateOfResidenceScheme) {
                throw IllegalArgumentException("credential")
            }
            return when (storeEntry) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    TODO("Operation not yet supported")
                }

                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    CertificateOfResidenceCredentialSdJwtAdapter(
                        storeEntry.toAttributeMap(),
                    )
                }

                is SubjectCredentialStore.StoreEntry.Iso -> {
                    CertificateOfResidenceCredentialIsoMdocAdapter(
                        storeEntry.toNamespaceAttributeMap(),
                    )
                }
            }
        }
    }
}

private class CertificateOfResidenceCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : CertificateOfResidenceCredentialAdapter() {

    override val documentNumber: String?
        get() = attributes[DOCUMENT_NUMBER]?.contentOrNull

    override val issuingAuthority: String?
        get() = attributes[ISSUING_AUTHORITY]?.contentOrNull

    override val familyName: String?
        get() = attributes[FAMILY_NAME]?.contentOrNull

    override val givenName: String?
        get() = attributes[GIVEN_NAME]?.contentOrNull

    override val birthDate: LocalDate?
        get() = attributes[BIRTH_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val residenceAddress: String?
        get() = attributes[RESIDENCE_ADDRESS]?.contentOrNull

    override val gender: IsoIec5218Gender?
        get() = attributes[GENDER]?.contentOrNull
            ?.let { s -> IsoIec5218Gender.entries.firstOrNull { it.name == s } }

    override val birthPlace: String?
        get() = attributes[BIRTH_PLACE]?.contentOrNull

    override val arrivalDate: LocalDate?
        get() = attributes[ARRIVAL_DATE]?.contentOrNull?.toLocalDateOrNull()

    override val nationality: String?
        get() = attributes[NATIONALITY]?.contentOrNull

    override val administrativeNumber: String?
        get() = attributes[ADMINISTRATIVE_NUMBER]?.contentOrNull

    override val issuanceDate: Instant?
        get() = attributes[ISSUANCE_DATE]?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = attributes[EXPIRY_DATE]?.contentOrNull?.toInstantOrNull()

    override val issuingCountry: String?
        get() = attributes[ISSUING_COUNTRY]?.contentOrNull

    override val issuingJurisdiction: String?
        get() = attributes[ISSUING_JURISDICTION]?.contentOrNull
}

private class CertificateOfResidenceCredentialIsoMdocAdapter(
    namespaces: Map<String, Map<String, Any>>?,
) : CertificateOfResidenceCredentialAdapter() {
    private val namespace =
        namespaces?.get(CertificateOfResidenceScheme.toString())
            ?: throw IllegalArgumentException("namespaces") // contains required attributes


    override val documentNumber: String?
        get() = namespace[DOCUMENT_NUMBER] as String?

    override val issuingAuthority: String?
        get() = namespace[ISSUING_AUTHORITY] as String?

    override val familyName: String?
        get() = namespace[FAMILY_NAME] as String?

    override val givenName: String?
        get() = namespace[GIVEN_NAME] as String?

    override val birthDate: LocalDate?
        get() = namespace[BIRTH_DATE].toLocalDateOrNull()

    override val residenceAddress: String?
        get() = namespace[RESIDENCE_ADDRESS] as String?

    override val gender: IsoIec5218Gender?
        get() = namespace[GENDER]
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

    override val birthPlace: String?
        get() = namespace[BIRTH_PLACE] as String?

    override val arrivalDate: LocalDate?
        get() = namespace[ARRIVAL_DATE].toLocalDateOrNull()

    override val nationality: String?
        get() = namespace[NATIONALITY] as String?

    override val administrativeNumber: String?
        get() = namespace[ADMINISTRATIVE_NUMBER] as String?

    override val issuanceDate: Instant?
        get() = namespace[ISSUANCE_DATE].toInstantOrNull()

    override val expiryDate: Instant?
        get() = namespace[EXPIRY_DATE].toInstantOrNull()

    override val issuingCountry: String?
        get() = namespace[ISSUING_COUNTRY] as String?

    override val issuingJurisdiction: String?
        get() = namespace[ISSUING_JURISDICTION] as String?
}
