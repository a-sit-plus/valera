package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_birth_city
import at.asitplus.valera.resources.attribute_friendly_name_birth_country
import at.asitplus.valera.resources.attribute_friendly_name_birth_place
import at.asitplus.valera.resources.attribute_friendly_name_birth_state
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_email_address
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_family_name_birth
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_given_name_birth
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_location_status
import at.asitplus.valera.resources.attribute_friendly_name_main_address
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_city
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_country
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_house_number
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_postal_code
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_state
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_street
import at.asitplus.valera.resources.attribute_friendly_name_mobile_phone_number
import at.asitplus.valera.resources.attribute_friendly_name_nationality
import at.asitplus.valera.resources.attribute_friendly_name_personal_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_place_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.attribute_friendly_name_sex
import at.asitplus.valera.resources.attribute_friendly_name_trust_anchor
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.app.common.minus
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements.Address
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements.PlaceOfBirth
import org.jetbrains.compose.resources.StringResource
import at.asitplus.wallet.eupid.EuPidDataElements as Attributes


class EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translateSingleClaimReference(claimReference: SingleClaimReference) = when (claimReference) {
        is JsonClaimReference -> sdJwtPathLabel(claimReference.normalizedJsonPath)

        is MdocClaimReference -> mdocClaimLabel(claimReference.namespace, claimReference.claimName)
    }

    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        attributeName.minus(EU_PID_DOCTYPE).let {
            it.memberName(0)?.let { claim ->
                mdocClaimLabel(EU_PID_DOCTYPE, claim) ?: sdJwtPathLabel(it)
            } ?: sdJwtPathLabel(it)
        }

    @Suppress("DEPRECATION")
    private fun mdocClaimLabel(namespace: String, claimName: String): StringResource? =
        if (namespace != EU_PID_DOCTYPE) null else with(Attributes) {
            when (claimName) {
                FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                PORTRAIT -> Res.string.attribute_friendly_name_portrait
                FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                PLACE_OF_BIRTH -> Res.string.attribute_friendly_name_place_of_birth
                RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
                RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
                RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
                RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                RESIDENT_STREET -> Res.string.attribute_friendly_name_main_residence_street
                RESIDENT_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
                SEX -> Res.string.attribute_friendly_name_sex
                NATIONALITY -> Res.string.attribute_friendly_name_nationality
                ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
                EMAIL_ADDRESS -> Res.string.attribute_friendly_name_email_address
                MOBILE_PHONE_NUMBER -> Res.string.attribute_friendly_name_mobile_phone_number
                TRUST_ANCHOR -> Res.string.attribute_friendly_name_trust_anchor
                LOCATION_STATUS -> Res.string.attribute_friendly_name_location_status
                else -> null
            }
        }

    private fun sdJwtPathLabel(path: NormalizedJsonPath): StringResource? = with(EuPidSdJwtDataElements) {
        when (val first = path.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                PORTRAIT -> Res.string.attribute_friendly_name_portrait
                FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                PREFIX_PLACE_OF_BIRTH -> placeOfBirthLabel(path)
                PLACE_OF_BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
                PLACE_OF_BIRTH_REGION -> Res.string.attribute_friendly_name_birth_state
                PLACE_OF_BIRTH_LOCALITY -> Res.string.attribute_friendly_name_birth_city
                PREFIX_ADDRESS -> addressLabel(path)
                ADDRESS_FORMATTED -> Res.string.attribute_friendly_name_main_address
                ADDRESS_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                ADDRESS_REGION -> Res.string.attribute_friendly_name_main_residence_state
                ADDRESS_LOCALITY -> Res.string.attribute_friendly_name_main_residence_city
                ADDRESS_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                ADDRESS_STREET -> Res.string.attribute_friendly_name_main_residence_street
                ADDRESS_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
                SEX -> Res.string.attribute_friendly_name_sex
                NATIONALITIES -> Res.string.attribute_friendly_name_nationality
                ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
                EMAIL -> Res.string.attribute_friendly_name_email_address
                PHONE_NUMBER -> Res.string.attribute_friendly_name_mobile_phone_number
                TRUST_ANCHOR -> Res.string.attribute_friendly_name_trust_anchor
                else -> null
            }

            else -> null
        }
    }

    private fun placeOfBirthLabel(path: NormalizedJsonPath): StringResource? =
        when (path.memberName(1)) {
            PlaceOfBirth.COUNTRY -> Res.string.attribute_friendly_name_birth_country
            PlaceOfBirth.REGION -> Res.string.attribute_friendly_name_birth_state
            PlaceOfBirth.LOCALITY -> Res.string.attribute_friendly_name_birth_city
            null -> Res.string.attribute_friendly_name_birth_place
            else -> null
        }

    private fun addressLabel(path: NormalizedJsonPath): StringResource? =
        when (path.memberName(1)) {
            Address.FORMATTED -> Res.string.attribute_friendly_name_main_address
            Address.COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
            Address.REGION -> Res.string.attribute_friendly_name_main_residence_state
            Address.LOCALITY -> Res.string.attribute_friendly_name_main_residence_city
            Address.POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
            Address.STREET -> Res.string.attribute_friendly_name_main_residence_street
            Address.HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
            null -> Res.string.attribute_friendly_name_main_address
            else -> null
        }
}
