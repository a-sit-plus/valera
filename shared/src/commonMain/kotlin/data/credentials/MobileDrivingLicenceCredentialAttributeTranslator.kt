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
import at.asitplus.valera.resources.attribute_friendly_name_biometric_template_face
import at.asitplus.valera.resources.attribute_friendly_name_biometric_template_finger
import at.asitplus.valera.resources.attribute_friendly_name_biometric_template_iris
import at.asitplus.valera.resources.attribute_friendly_name_biometric_template_signature_sign
import at.asitplus.valera.resources.attribute_friendly_name_birth_place
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_distinguishing_sign
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_driving_privileges
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_eye_colour
import at.asitplus.valera.resources.attribute_friendly_name_family_name_national_character
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_given_name_national_character
import at.asitplus.valera.resources.attribute_friendly_name_hair_colour
import at.asitplus.valera.resources.attribute_friendly_name_height
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_main_address
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_city
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_country
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_postal_code
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_state
import at.asitplus.valera.resources.attribute_friendly_name_nationality
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.attribute_friendly_name_portrait_capture_date
import at.asitplus.valera.resources.attribute_friendly_name_sex
import at.asitplus.valera.resources.attribute_friendly_name_signature_usual_mark
import at.asitplus.valera.resources.attribute_friendly_name_weight
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import org.jetbrains.compose.resources.StringResource


object MobileDrivingLicenceCredentialAttributeTranslator : CredentialAttributeTranslator {

    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        attributeName.segments.firstOrNull()?.memberName()?.let { getFromIsoName(it) }
            ?: attributeName.segments.lastOrNull()?.memberName()?.let { getFromIsoName(it) }

    private fun NormalizedJsonPathSegment.memberName() = when (this) {
        is NormalizedJsonPathSegment.NameSegment -> this.memberName
        else -> null
    }

    private fun getFromIsoName(claimName: String): StringResource? = with(MobileDrivingLicenceDataElements) {
        when (claimName) {
            FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
            GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
            BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
            ISSUE_DATE -> Res.string.attribute_friendly_name_issue_date
            EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
            ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
            ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
            DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
            PORTRAIT -> Res.string.attribute_friendly_name_portrait
            DRIVING_PRIVILEGES -> Res.string.attribute_friendly_name_driving_privileges
            UN_DISTINGUISHING_SIGN -> Res.string.attribute_friendly_name_distinguishing_sign
            ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
            SEX -> Res.string.attribute_friendly_name_sex
            HEIGHT -> Res.string.attribute_friendly_name_height
            WEIGHT -> Res.string.attribute_friendly_name_weight
            EYE_COLOUR -> Res.string.attribute_friendly_name_eye_colour
            HAIR_COLOUR -> Res.string.attribute_friendly_name_hair_colour
            BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
            RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
            PORTRAIT_CAPTURE_DATE -> Res.string.attribute_friendly_name_portrait_capture_date
            AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
            AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
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
            ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
            NATIONALITY -> Res.string.attribute_friendly_name_nationality
            RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
            RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
            RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
            RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
            FAMILY_NAME_NATIONAL_CHARACTER -> Res.string.attribute_friendly_name_family_name_national_character
            GIVEN_NAME_NATIONAL_CHARACTER -> Res.string.attribute_friendly_name_given_name_national_character
            SIGNATURE_USUAL_MARK -> Res.string.attribute_friendly_name_signature_usual_mark
            BIOMETRIC_TEMPLATE_FINGER -> Res.string.attribute_friendly_name_biometric_template_finger
            BIOMETRIC_TEMPLATE_FACE -> Res.string.attribute_friendly_name_biometric_template_face
            BIOMETRIC_TEMPLATE_IRIS -> Res.string.attribute_friendly_name_biometric_template_iris
            BIOMETRIC_TEMPLATE_SIGNATURE_SIGN -> Res.string.attribute_friendly_name_biometric_template_signature_sign
            else -> null
        }
    }
}
