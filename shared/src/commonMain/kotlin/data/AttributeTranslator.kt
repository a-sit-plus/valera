package data

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


class AttributeTranslator(val credentialScheme: ConstantIndex.CredentialScheme) {
    fun translate(attributeName: NormalizedJsonPath): StringResource? {
        return when (credentialScheme) {
            is IdAustriaScheme -> attributeName.segments.firstOrNull()?.let { first ->
                when (first) {
                    is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                        IdAustriaScheme.Attributes.BPK -> Res.string.attribute_friendly_name_bpk
                        IdAustriaScheme.Attributes.FIRSTNAME -> Res.string.attribute_friendly_name_firstname
                        IdAustriaScheme.Attributes.LASTNAME -> Res.string.attribute_friendly_name_lastname
                        IdAustriaScheme.Attributes.DATE_OF_BIRTH -> Res.string.attribute_friendly_name_date_of_birth
                        IdAustriaScheme.Attributes.PORTRAIT -> Res.string.attribute_friendly_name_portrait
                        IdAustriaScheme.Attributes.AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                        IdAustriaScheme.Attributes.AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                        IdAustriaScheme.Attributes.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                        IdAustriaScheme.Attributes.AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                        IdAustriaScheme.Attributes.MAIN_ADDRESS -> {
                            val second = attributeName.segments.getOrNull(1)
                            if (second == null) {
                                Res.string.attribute_friendly_name_main_address
                            } else when (second) {
                                is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                                    IdAustriaCredentialMainAddress.GEMEINDEKENNZIFFER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_code
                                    IdAustriaCredentialMainAddress.GEMEINDEBEZEICHNUNG -> Res.string.id_austria_credential_attribute_friendly_name_main_address_municipality_name
                                    IdAustriaCredentialMainAddress.POSTLEITZAHL -> Res.string.id_austria_credential_attribute_friendly_name_main_address_postal_code
                                    IdAustriaCredentialMainAddress.ORTSCHAFT -> Res.string.id_austria_credential_attribute_friendly_name_main_address_location
                                    IdAustriaCredentialMainAddress.STRASSE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_street
                                    IdAustriaCredentialMainAddress.HAUSNUMMER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_house_number
                                    IdAustriaCredentialMainAddress.STIEGE -> Res.string.id_austria_credential_attribute_friendly_name_main_address_stair
                                    IdAustriaCredentialMainAddress.TUER -> Res.string.id_austria_credential_attribute_friendly_name_main_address_door
                                    else -> null
                                }

                                else -> null
                            }
                        }
                        else -> null
                    }

                    else -> null
                }
            }

            is EuPidScheme -> attributeName.segments.firstOrNull()?.let { first ->
                when (first) {
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

            is MobileDrivingLicenceScheme -> attributeName.segments.firstOrNull()?.let { first ->
                when (first) {
                    is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                        MobileDrivingLicenceDataElements.GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                        MobileDrivingLicenceDataElements.FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                        MobileDrivingLicenceDataElements.PORTRAIT -> Res.string.attribute_friendly_name_portrait
                        MobileDrivingLicenceDataElements.BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                        MobileDrivingLicenceDataElements.AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                        MobileDrivingLicenceDataElements.RESIDENT_ADDRESS -> Res.string.attribute_friendly_name_main_address
                        MobileDrivingLicenceDataElements.RESIDENT_CITY -> Res.string.attribute_friendly_name_main_residence_city
                        MobileDrivingLicenceDataElements.RESIDENT_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                        MobileDrivingLicenceDataElements.RESIDENT_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                        MobileDrivingLicenceDataElements.RESIDENT_STATE -> Res.string.attribute_friendly_name_main_residence_state
                        MobileDrivingLicenceDataElements.NATIONALITY -> Res.string.attribute_friendly_name_nationality
                        MobileDrivingLicenceDataElements.AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
                        MobileDrivingLicenceDataElements.AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
                        MobileDrivingLicenceDataElements.BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
                        MobileDrivingLicenceDataElements.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
                        MobileDrivingLicenceDataElements.ISSUE_DATE -> Res.string.attribute_friendly_name_issue_date
                        MobileDrivingLicenceDataElements.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
                        MobileDrivingLicenceDataElements.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
                        MobileDrivingLicenceDataElements.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
                        MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES -> Res.string.attribute_friendly_name_driving_privileges
                        MobileDrivingLicenceDataElements.UN_DISTINGUISHING_SIGN -> Res.string.attribute_friendly_name_distinguishing_sign
                        else -> null
                    }

                    else -> null
                }
            }

            else -> null
        }
    }
}