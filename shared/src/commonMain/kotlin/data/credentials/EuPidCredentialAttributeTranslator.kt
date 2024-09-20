package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import compose_wallet_app.shared.generated.resources.Res
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_at_least_14
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_at_least_16
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_at_least_18
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_at_least_21
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_birth_year
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_age_in_years
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_city
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_place
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_birth_state
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_bpk
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_date_of_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_distinguishing_sign
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_document_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_driving_privileges
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_expiry_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_family_name_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_firstname
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_given_name_birth
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issue_date
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_authority
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_issuing_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_lastname
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_address
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_city
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_country
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_house_number
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_postal_code
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_state
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_main_residence_street
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_nationality
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_portrait
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_sex
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_door
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_house_number
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_location
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_code
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_name
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_postal_code
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_stair
import compose_wallet_app.shared.generated.resources.id_austria_credential_attribute_friendly_name_main_address_street
import data.credentials.IdAustriaCredentialMainAddress
import org.jetbrains.compose.resources.ExperimentalResourceApi
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
