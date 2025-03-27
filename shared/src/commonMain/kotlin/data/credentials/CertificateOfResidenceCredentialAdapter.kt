package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ADMINISTRATIVE_NUMBER
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.ARRIVAL_DATE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.Address
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
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_ADMIN_UNIT_L_1
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_ADMIN_UNIT_L_2
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_FULL_ADDRESS
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_LOCATOR_NAME
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_POST_CODE
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_POST_NAME
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_PO_BOX
import at.asitplus.wallet.cor.CertificateOfResidenceDataElements.RESIDENCE_ADDRESS_THOROUGHFARE
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eupid.IsoIec5218Gender
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import data.Attribute
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
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
                RESIDENCE_ADDRESS -> with(CertificateOfResidenceDataElements.Address) {
                    when (val second = path.segments.drop(1).firstOrNull()) {
                        is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                            PO_BOX -> Attribute.fromValue(residenceAddressPoBox)
                            THOROUGHFARE -> Attribute.fromValue(residenceAddressThoroughfare)
                            LOCATOR_DESIGNATOR -> Attribute.fromValue(residenceAddressLocatorDesignator)
                            LOCATOR_NAME -> Attribute.fromValue(residenceAddressLocatorName)
                            POST_CODE -> Attribute.fromValue(residenceAddressPostCode)
                            POST_NAME -> Attribute.fromValue(residenceAddressPostName)
                            ADMIN_UNIT_L_1 -> Attribute.fromValue(residenceAddressAdminUnitL1)
                            ADMIN_UNIT_L_2 -> Attribute.fromValue(residenceAddressAdminUnitL2)
                            FULL_ADDRESS -> Attribute.fromValue(residenceAddressFullAddress)
                            else -> null
                        }

                        else -> Attribute.fromValue(residenceAddress)
                    }
                }

                RESIDENCE_ADDRESS_PO_BOX -> Attribute.fromValue(residenceAddressPoBox)
                RESIDENCE_ADDRESS_THOROUGHFARE -> Attribute.fromValue(residenceAddressThoroughfare)
                RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR -> Attribute.fromValue(residenceAddressLocatorDesignator)
                RESIDENCE_ADDRESS_LOCATOR_NAME -> Attribute.fromValue(residenceAddressLocatorName)
                RESIDENCE_ADDRESS_POST_CODE -> Attribute.fromValue(residenceAddressPostCode)
                RESIDENCE_ADDRESS_POST_NAME -> Attribute.fromValue(residenceAddressPostName)
                RESIDENCE_ADDRESS_ADMIN_UNIT_L_1 -> Attribute.fromValue(residenceAddressAdminUnitL1)
                RESIDENCE_ADDRESS_ADMIN_UNIT_L_2 -> Attribute.fromValue(residenceAddressAdminUnitL2)
                RESIDENCE_ADDRESS_FULL_ADDRESS -> Attribute.fromValue(residenceAddressFullAddress)
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
    abstract val residenceAddressPoBox: String?
    abstract val residenceAddressThoroughfare: String?
    abstract val residenceAddressLocatorDesignator: String?
    abstract val residenceAddressLocatorName: String?
    abstract val residenceAddressPostCode: String?
    abstract val residenceAddressPostName: String?
    abstract val residenceAddressAdminUnitL1: String?
    abstract val residenceAddressAdminUnitL2: String?
    abstract val residenceAddressFullAddress: String?
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
                is SubjectCredentialStore.StoreEntry.SdJwt -> storeEntry.toComplexJson()
                    ?.let { CertificateOfResidenceComplexCredentialSdJwtAdapter(it) }
                    ?: CertificateOfResidenceCredentialSdJwtAdapter(storeEntry.toAttributeMap())

                else -> TODO("Operation not yet supported")
            }
        }
    }
}

private class CertificateOfResidenceCredentialSdJwtAdapter(
    private val attributes: Map<String, JsonPrimitive>,
) : CertificateOfResidenceCredentialAdapter() {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

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

    override val residenceAddressPoBox: String?
        get() = attributes[RESIDENCE_ADDRESS_PO_BOX]?.contentOrNull

    override val residenceAddressThoroughfare: String?
        get() = attributes[RESIDENCE_ADDRESS_THOROUGHFARE]?.contentOrNull

    override val residenceAddressLocatorDesignator: String?
        get() = attributes[RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR]?.contentOrNull

    override val residenceAddressLocatorName: String?
        get() = attributes[RESIDENCE_ADDRESS_LOCATOR_NAME]?.contentOrNull

    override val residenceAddressPostCode: String?
        get() = attributes[RESIDENCE_ADDRESS_POST_CODE]?.contentOrNull

    override val residenceAddressPostName: String?
        get() = attributes[RESIDENCE_ADDRESS_POST_NAME]?.contentOrNull

    override val residenceAddressAdminUnitL1: String?
        get() = attributes[RESIDENCE_ADDRESS_ADMIN_UNIT_L_1]?.contentOrNull

    override val residenceAddressAdminUnitL2: String?
        get() = attributes[RESIDENCE_ADDRESS_ADMIN_UNIT_L_2]?.contentOrNull

    override val residenceAddressFullAddress: String?
        get() = attributes[RESIDENCE_ADDRESS_FULL_ADDRESS]?.contentOrNull

    override val gender: IsoIec5218Gender?
        get() = attributes[GENDER]?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() } }
            ?: attributes[GENDER]?.contentOrNull?.toUIntOrNull()
                ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

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


private class CertificateOfResidenceComplexCredentialSdJwtAdapter(
    private val attributes: JsonObject,
) : CertificateOfResidenceCredentialAdapter() {
    override val representation: CredentialRepresentation
        get() = CredentialRepresentation.SD_JWT

    override val documentNumber: String?
        get() = (attributes[DOCUMENT_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuingAuthority: String?
        get() = (attributes[ISSUING_AUTHORITY] as? JsonPrimitive?)?.contentOrNull

    override val familyName: String?
        get() = (attributes[FAMILY_NAME] as? JsonPrimitive?)?.contentOrNull

    override val givenName: String?
        get() = (attributes[GIVEN_NAME] as? JsonPrimitive?)?.contentOrNull

    override val birthDate: LocalDate?
        get() = (attributes[BIRTH_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val residenceAddress: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressPoBox: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.PO_BOX] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_PO_BOX] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressThoroughfare: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.THOROUGHFARE] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_THOROUGHFARE] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressLocatorDesignator: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.LOCATOR_DESIGNATOR] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressLocatorName: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.LOCATOR_NAME] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_LOCATOR_NAME] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressPostCode: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.POST_CODE] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_POST_CODE] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressPostName: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.POST_NAME] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_POST_NAME] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressAdminUnitL1: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.ADMIN_UNIT_L_1] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_ADMIN_UNIT_L_1] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressAdminUnitL2: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.ADMIN_UNIT_L_2] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_ADMIN_UNIT_L_2] as? JsonPrimitive?)?.contentOrNull

    override val residenceAddressFullAddress: String?
        get() = (attributes[RESIDENCE_ADDRESS] as? JsonObject?)?.let {
            (it[Address.FULL_ADDRESS] as? JsonPrimitive?)?.contentOrNull
        } ?: (attributes[RESIDENCE_ADDRESS_FULL_ADDRESS] as? JsonPrimitive?)?.contentOrNull

    override val gender: IsoIec5218Gender?
        get() = (attributes[GENDER] as? JsonPrimitive?)?.contentOrNull?.toIntOrNull()
            ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code.toUInt() } }
            ?: (attributes[GENDER] as? JsonPrimitive?)?.contentOrNull?.toUIntOrNull()
                ?.let { code -> IsoIec5218Gender.entries.firstOrNull { it.code == code } }

    override val birthPlace: String?
        get() = (attributes[BIRTH_PLACE] as? JsonPrimitive?)?.contentOrNull

    override val arrivalDate: LocalDate?
        get() = (attributes[ARRIVAL_DATE] as? JsonPrimitive?)?.contentOrNull?.toLocalDateOrNull()

    override val nationality: String?
        get() = (attributes[NATIONALITY] as? JsonPrimitive?)?.contentOrNull

    override val administrativeNumber: String?
        get() = (attributes[ADMINISTRATIVE_NUMBER] as? JsonPrimitive?)?.contentOrNull

    override val issuanceDate: Instant?
        get() = (attributes[ISSUANCE_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val expiryDate: Instant?
        get() = (attributes[EXPIRY_DATE] as? JsonPrimitive?)?.contentOrNull?.toInstantOrNull()

    override val issuingCountry: String?
        get() = (attributes[ISSUING_COUNTRY] as? JsonPrimitive?)?.contentOrNull

    override val issuingJurisdiction: String?
        get() = (attributes[ISSUING_JURISDICTION] as? JsonPrimitive?)?.contentOrNull
}
