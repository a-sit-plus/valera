package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import composewalletapp.shared.generated.resources.Res
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
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
import composewalletapp.shared.generated.resources.attribute_friendly_name_family_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_firstname
import composewalletapp.shared.generated.resources.attribute_friendly_name_given_name_birth
import composewalletapp.shared.generated.resources.attribute_friendly_name_issue_date
import composewalletapp.shared.generated.resources.attribute_friendly_name_issuing_authority
import composewalletapp.shared.generated.resources.attribute_friendly_name_issuing_country
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
import composewalletapp.shared.generated.resources.attribute_friendly_name_sex
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


object EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                EuPidScheme.Attributes.GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                EuPidScheme.Attributes.FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                EuPidScheme.Attributes.BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                EuPidScheme.Attributes.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                EuPidScheme.Attributes.RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
                EuPidScheme.Attributes.RESIDENT_STREET -> Res.string.attribute_friendly_name_main_residence_street
                EuPidScheme.Attributes.RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
                EuPidScheme.Attributes.RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                EuPidScheme.Attributes.RESIDENT_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
                EuPidScheme.Attributes.RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                EuPidScheme.Attributes.RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
                EuPidScheme.Attributes.GENDER -> Res.string.attribute_friendly_name_sex
                EuPidScheme.Attributes.NATIONALITY -> Res.string.attribute_friendly_name_nationality
                EuPidScheme.Attributes.AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
                EuPidScheme.Attributes.AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
                EuPidScheme.Attributes.FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                EuPidScheme.Attributes.GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                EuPidScheme.Attributes.BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
                EuPidScheme.Attributes.BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
                EuPidScheme.Attributes.BIRTH_STATE -> Res.string.attribute_friendly_name_birth_state
                EuPidScheme.Attributes.BIRTH_CITY -> Res.string.attribute_friendly_name_birth_city
                else -> null
            }

            else -> null
        }
}