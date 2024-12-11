package data

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.COUNTRY_CODE
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.OTT
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.VALID_UNTIL
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_14
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_16
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_18
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_21
import at.asitplus.valera.resources.attribute_friendly_name_age_birth_year
import at.asitplus.valera.resources.attribute_friendly_name_age_in_years
import at.asitplus.valera.resources.attribute_friendly_name_birth_city
import at.asitplus.valera.resources.attribute_friendly_name_birth_country
import at.asitplus.valera.resources.attribute_friendly_name_birth_place
import at.asitplus.valera.resources.attribute_friendly_name_birth_state
import at.asitplus.valera.resources.attribute_friendly_name_bpk
import at.asitplus.valera.resources.attribute_friendly_name_country_code
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_distinguishing_sign
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_driving_privileges
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_eye_colour
import at.asitplus.valera.resources.attribute_friendly_name_family_name_birth
import at.asitplus.valera.resources.attribute_friendly_name_family_name_national_character
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_given_name_birth
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
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_house_number
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_postal_code
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_state
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_street
import at.asitplus.valera.resources.attribute_friendly_name_nationality
import at.asitplus.valera.resources.attribute_friendly_name_one_time_token
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.attribute_friendly_name_portrait_capture_date
import at.asitplus.valera.resources.attribute_friendly_name_sex
import at.asitplus.valera.resources.attribute_friendly_name_signature_usual_mark
import at.asitplus.valera.resources.attribute_friendly_name_valid_until
import at.asitplus.valera.resources.attribute_friendly_name_weight
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_door
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_house_number
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_location
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_code
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_municipality_name
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_postal_code
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_stair
import at.asitplus.valera.resources.id_austria_credential_attribute_friendly_name_main_address_street
import data.credentials.IdAustriaCredentialMainAddress
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
                            when (val second = attributeName.segments.getOrNull(1)) {
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

                                null -> Res.string.attribute_friendly_name_main_address

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

            is EPrescriptionScheme -> attributeName.segments.firstOrNull()?.let { first ->
                when (first) {
                    is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                        OTT -> Res.string.attribute_friendly_name_one_time_token
                        VALID_UNTIL -> Res.string.attribute_friendly_name_valid_until
                        COUNTRY_CODE -> Res.string.attribute_friendly_name_country_code
                        else -> null
                    }

                    else -> null
                }
            }

            is MobileDrivingLicenceScheme -> attributeName.segments.firstOrNull()?.let { first ->
                with(MobileDrivingLicenceDataElements) {
                    when (first) {
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

            else -> null
        }
    }
}
