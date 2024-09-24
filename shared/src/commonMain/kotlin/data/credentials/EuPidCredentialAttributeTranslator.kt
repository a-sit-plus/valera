package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_at_least_18
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_birth_year
import composewalletapp.shared.generated.resources.attribute_friendly_name_age_in_years
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_city
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_country
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_place
import composewalletapp.shared.generated.resources.attribute_friendly_name_birth_state
import composewalletapp.shared.generated.resources.attribute_friendly_name_date_of_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_family_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_firstname
import composewalletapp.shared.generated.resources.attribute_friendly_name_given_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_lastname
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_address
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_city
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_country
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_house_number
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_postal_code
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_state
import composewalletapp.shared.generated.resources.attribute_friendly_name_main_residence_street
import composewalletapp.shared.generated.resources.attribute_friendly_name_nationality
import composewalletapp.shared.generated.resources.attribute_friendly_name_sex
import org.jetbrains.compose.resources.StringResource


object EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(EuPidScheme.Attributes) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                    FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                    BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                    AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                    RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
                    RESIDENT_STREET -> Res.string.attribute_friendly_name_main_residence_street
                    RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
                    RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                    RESIDENT_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
                    RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                    RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
                    GENDER -> Res.string.attribute_friendly_name_sex
                    NATIONALITY -> Res.string.attribute_friendly_name_nationality
                    AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
                    AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
                    FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                    GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                    BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
                    BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
                    BIRTH_STATE -> Res.string.attribute_friendly_name_birth_state
                    BIRTH_CITY -> Res.string.attribute_friendly_name_birth_city
                    else -> null
                }

                else -> null
            }
        }
}