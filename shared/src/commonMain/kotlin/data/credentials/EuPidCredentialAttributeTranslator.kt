@file:Suppress("DEPRECATION")

package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_12
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_13
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_14
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_16
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_18
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_21
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_25
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_60
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_62
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_65
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_68
import at.asitplus.valera.resources.attribute_friendly_name_age_birth_year
import at.asitplus.valera.resources.attribute_friendly_name_age_in_years
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
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.attribute_friendly_name_portrait_capture_date
import at.asitplus.valera.resources.attribute_friendly_name_sex
import at.asitplus.valera.resources.attribute_friendly_name_trust_anchor
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.Address
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.AgeEqualOrOver
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.PlaceOfBirth
import org.jetbrains.compose.resources.StringResource


object EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {

    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        withIsoNames(attributeName) ?: withSdJwtNames(attributeName)

    private fun withIsoNames(attributeName: NormalizedJsonPath): StringResource? =
        attributeName.segments.firstOrNull()?.memberName()?.let { getFromIsoName(it) }
            ?: attributeName.segments.lastOrNull()?.memberName()?.let { getFromIsoName(it) }

    private fun NormalizedJsonPathSegment.memberName() = when(this) {
        is NormalizedJsonPathSegment.NameSegment -> this.memberName
        else -> null
    }

    private fun getFromIsoName(claimName: String): StringResource? = with(EuPidScheme.Attributes) {
        when (claimName) {
            FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
            GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
            BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
            PORTRAIT -> Res.string.attribute_friendly_name_portrait
            AGE_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
            AGE_OVER_13 -> Res.string.attribute_friendly_name_age_at_least_13
            AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
            AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
            AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
            AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
            AGE_OVER_25 -> Res.string.attribute_friendly_name_age_at_least_25
            AGE_OVER_60 -> Res.string.attribute_friendly_name_age_at_least_60
            AGE_OVER_62 -> Res.string.attribute_friendly_name_age_at_least_62
            AGE_OVER_65 -> Res.string.attribute_friendly_name_age_at_least_65
            AGE_OVER_68 -> Res.string.attribute_friendly_name_age_at_least_68
            AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
            AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
            FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
            GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
            BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
            BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
            BIRTH_STATE -> Res.string.attribute_friendly_name_birth_state
            BIRTH_CITY -> Res.string.attribute_friendly_name_birth_city
            RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
            RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
            RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
            RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
            RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
            RESIDENT_STREET -> Res.string.attribute_friendly_name_main_residence_street
            RESIDENT_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
            GENDER -> Res.string.attribute_friendly_name_sex
            SEX -> Res.string.attribute_friendly_name_sex
            NATIONALITY -> Res.string.attribute_friendly_name_nationality
            ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
            EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
            ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
            DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
            ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
            ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
            ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
            PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
            EMAIL_ADDRESS -> Res.string.attribute_friendly_name_email_address
            MOBILE_PHONE_NUMBER -> Res.string.attribute_friendly_name_mobile_phone_number
            TRUST_ANCHOR -> Res.string.attribute_friendly_name_trust_anchor
            LOCATION_STATUS -> Res.string.attribute_friendly_name_location_status
            PORTRAIT_CAPTURE_DATE -> Res.string.attribute_friendly_name_portrait_capture_date
            else -> null
        }
    }

    private fun withSdJwtNames(attributeName: NormalizedJsonPath) = with(EuPidSdJwtScheme.SdJwtAttributes) {
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                PORTRAIT -> Res.string.attribute_friendly_name_portrait
                PREFIX_AGE_EQUAL_OR_OVER -> when (val second = attributeName.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        AgeEqualOrOver.EQUAL_OR_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
                        AgeEqualOrOver.EQUAL_OR_OVER_13 -> Res.string.attribute_friendly_name_age_at_least_13
                        AgeEqualOrOver.EQUAL_OR_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                        AgeEqualOrOver.EQUAL_OR_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                        AgeEqualOrOver.EQUAL_OR_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                        AgeEqualOrOver.EQUAL_OR_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                        AgeEqualOrOver.EQUAL_OR_OVER_25 -> Res.string.attribute_friendly_name_age_at_least_25
                        AgeEqualOrOver.EQUAL_OR_OVER_60 -> Res.string.attribute_friendly_name_age_at_least_60
                        AgeEqualOrOver.EQUAL_OR_OVER_62 -> Res.string.attribute_friendly_name_age_at_least_62
                        AgeEqualOrOver.EQUAL_OR_OVER_65 -> Res.string.attribute_friendly_name_age_at_least_65
                        AgeEqualOrOver.EQUAL_OR_OVER_68 -> Res.string.attribute_friendly_name_age_at_least_68
                        else -> null
                    }
                    else -> null
                }
                AGE_EQUAL_OR_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
                AGE_EQUAL_OR_OVER_13 -> Res.string.attribute_friendly_name_age_at_least_13
                AGE_EQUAL_OR_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                AGE_EQUAL_OR_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                AGE_EQUAL_OR_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                AGE_EQUAL_OR_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                AGE_EQUAL_OR_OVER_25 -> Res.string.attribute_friendly_name_age_at_least_25
                AGE_EQUAL_OR_OVER_60 -> Res.string.attribute_friendly_name_age_at_least_60
                AGE_EQUAL_OR_OVER_62 -> Res.string.attribute_friendly_name_age_at_least_62
                AGE_EQUAL_OR_OVER_65 -> Res.string.attribute_friendly_name_age_at_least_65
                AGE_EQUAL_OR_OVER_68 -> Res.string.attribute_friendly_name_age_at_least_68
                AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
                AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
                FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                PREFIX_PLACE_OF_BIRTH -> when (val second = attributeName.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        PlaceOfBirth.COUNTRY -> Res.string.attribute_friendly_name_birth_country
                        PlaceOfBirth.REGION -> Res.string.attribute_friendly_name_birth_state
                        PlaceOfBirth.LOCALITY -> Res.string.attribute_friendly_name_birth_city
                        else -> null
                    }
                    else -> null
                }
                PLACE_OF_BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
                PLACE_OF_BIRTH_REGION -> Res.string.attribute_friendly_name_birth_state
                PLACE_OF_BIRTH_LOCALITY -> Res.string.attribute_friendly_name_birth_city
                PREFIX_ADDRESS -> when (val second = attributeName.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        Address.FORMATTED -> Res.string.attribute_friendly_name_main_address
                        Address.COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                        Address.REGION -> Res.string.attribute_friendly_name_main_residence_state
                        Address.LOCALITY -> Res.string.attribute_friendly_name_main_residence_city
                        Address.POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                        Address.STREET -> Res.string.attribute_friendly_name_main_residence_street
                        Address.HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number

                        else -> null
                    }
                    else -> null
                }
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
}
