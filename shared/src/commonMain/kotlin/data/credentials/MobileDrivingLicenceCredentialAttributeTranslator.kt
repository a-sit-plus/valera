package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import composewalletapp.shared.generated.resources.Res
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import composewalletapp.shared.generated.resources.attribute_friendly_name_administrative_number
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_14
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_16
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_18
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_21
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_birth_year
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_in_years
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_city
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_country
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_place
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_state
import composewalletapp.shared.generated.resources.attribute_friendly_name_bpk
import composewalletapp.shared.generated.resources.attribute_friendly_name_date_of_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_distinguishing_sign
import composewalletapp.shared.generated.resources.attribute_friendly_name_document_number
import composewalletapp.shared.generated.resources.attribute_friendly_name_driving_privileges
import composewalletapp.shared.generated.resources.attribute_friendly_name_expiry_date
import composewalletapp.shared.generated.resources.attribute_friendly_name_eye_colour
import composewalletapp.shared.generated.resources.attribute_friendly_name_family_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_family_name_national_character
import composewalletapp.shared.generated.resources.attribute_friendly_name_firstname
import composewalletapp.shared.generated.resources.attribute_friendly_name_given_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_given_name_national_character
import composewalletapp.shared.generated.resources.attribute_friendly_name_hair_colour
import composewalletapp.shared.generated.resources.attribute_friendly_name_height
import composewalletapp.shared.generated.resources.attribute_friendly_name_issue_date
import composewalletapp.shared.generated.resources.attribute_friendly_name_issuing_authority
import composewalletapp.shared.generated.resources.attribute_friendly_name_issuing_country
import composewalletapp.shared.generated.resources.attribute_friendly_name_issuing_jurisdiction
import composewalletapp.shared.generated.resources.attribute_friendly_name_lastname
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_address
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_city
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_country
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_house_number
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_postal_code
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_state
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_street
import composewalletapp.shared.generated.resources.attribute_friendly_name_nationality
import composewalletapp.shared.generated.resources.attribute_friendly_name_portrait
import composewalletapp.shared.generated.resources.attribute_friendly_name_portrait_capture_date
import composewalletapp.shared.generated.resources.attribute_friendly_name_sex
import composewalletapp.shared.generated.resources.attribute_friendly_name_signature_usual_mark
import composewalletapp.shared.generated.resources.attribute_friendly_name_weight
import composewalletapp.shared.generated.resources.error_feature_not_yet_available
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_door
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_house_number
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_location
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_code
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_name
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_postal_code
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_stair
import composewalletapp.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_street
import data.credentials.IdAustriaCredentialMainAddress
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource


object MobileDrivingLicenceCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(MobileDrivingLicenceDataElements) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
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
                    AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                    ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                    NATIONALITY -> Res.string.attribute_friendly_name_nationality
                    RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
                    RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
                    RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                    RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                    FAMILY_NAME_NATIONAL_CHARACTER -> Res.string.attribute_friendly_name_family_name_national_character
                    GIVEN_NAME_NATIONAL_CHARACTER -> Res.string.attribute_friendly_name_given_name_national_character
                    SIGNATURE_USUAL_MARK -> Res.string.attribute_friendly_name_signature_usual_mark
                    else -> null
                }

                else -> null
            }
        }
}