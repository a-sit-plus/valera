package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_administrative_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_at_least_18
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_birth_year
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_in_years
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_city
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_place
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_state
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_date_of_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_document_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_expiry_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_family_name_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_firstname
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_given_name_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issue_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_authority
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_jurisdiction
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_lastname
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_address
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_city
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_house_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_postal_code
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_state
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_street
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_nationality
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_sex
import org.jetbrains.compose.resources.StringResource


object EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        with(EuPidScheme.Attributes) {
            when (val first = attributeName.segments.firstOrNull()) {
                is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                    FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                    GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                    BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                    AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
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
                    NATIONALITY -> Res.string.attribute_friendly_name_nationality
                    ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
                    EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                    ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                    DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                    ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
                    ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                    ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
                    else -> null
                }

                else -> null
            }
        }
}
