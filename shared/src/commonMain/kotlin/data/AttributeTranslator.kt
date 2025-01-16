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
import at.asitplus.valera.resources.attribute_friendly_name_age_at_least_12
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
                with(IdAustriaScheme.Attributes) {
                    when (first) {
                        is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                            BPK -> Res.string.attribute_friendly_name_bpk
                            FIRSTNAME -> Res.string.attribute_friendly_name_firstname
                            LASTNAME -> Res.string.attribute_friendly_name_lastname
                            DATE_OF_BIRTH -> Res.string.attribute_friendly_name_date_of_birth
                            PORTRAIT -> Res.string.attribute_friendly_name_portrait
                            AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                            AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                            AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                            AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                            MAIN_ADDRESS -> {
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
            }

            is EuPidScheme -> attributeName.segments.firstOrNull()?.let { first ->
                euPidIsoNames(first) ?: euPidSdJwtNames(first)
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
                            AGE_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
                            AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                            AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                            AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                            AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
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

    private fun euPidIsoNames(first: NormalizedJsonPathSegment) = with(EuPidScheme.Attributes) {
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                AGE_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
                AGE_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                AGE_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                AGE_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                AGE_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
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
    private fun euPidSdJwtNames(first: NormalizedJsonPathSegment) = with(EuPidScheme.SdJwtAttributes) {
        when (first) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
                FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
                BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
                AGE_EQUAL_OR_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
                AGE_EQUAL_OR_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
                AGE_EQUAL_OR_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
                AGE_EQUAL_OR_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
                AGE_EQUAL_OR_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
                ADDRESS_FORMATTED -> Res.string.attribute_friendly_name_main_address
                ADDRESS_STREET -> Res.string.attribute_friendly_name_main_residence_street
                ADDRESS_LOCALITY -> Res.string.attribute_friendly_name_main_residence_city
                ADDRESS_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
                ADDRESS_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
                ADDRESS_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
                ADDRESS_REGION -> Res.string.attribute_friendly_name_main_residence_state
                GENDER -> Res.string.attribute_friendly_name_sex
                NATIONALITIES -> Res.string.attribute_friendly_name_nationality
                AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
                AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
                FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
                GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
                PLACE_OF_BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
                PLACE_OF_BIRTH_REGION -> Res.string.attribute_friendly_name_birth_state
                PLACE_OF_BIRTH_LOCALITY -> Res.string.attribute_friendly_name_birth_city
                else -> null
            }

            else -> null
        }
    }
}
