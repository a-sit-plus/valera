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
import at.asitplus.valera.resources.attribute_friendly_name_place_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_portrait
import at.asitplus.valera.resources.attribute_friendly_name_portrait_capture_date
import at.asitplus.valera.resources.attribute_friendly_name_sex
import at.asitplus.valera.resources.attribute_friendly_name_trust_anchor
import at.asitplus.wallet.eupid.EuPidScheme
import org.jetbrains.compose.resources.StringResource


class EuPidCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        (withIsoNames(attributeName) ?: withSdJwtNames(attributeName))?.stringResourceOrNull()

    private fun withIsoNames(attributeName: NormalizedJsonPath): EuPidCredentialClaimDefinition? = listOfNotNull(
        attributeName.segments.firstOrNull(),
        attributeName.segments.lastOrNull(),
    ).filterIsInstance<NormalizedJsonPathSegment.NameSegment>().firstNotNullOfOrNull {
        getFromIsoName(it.memberName)
    }

    private fun NormalizedJsonPathSegment.memberName() = when(this) {
        is NormalizedJsonPathSegment.NameSegment -> this.memberName
        else -> null
    }

    private fun getFromIsoName(
        claimName: String
    ): EuPidCredentialClaimDefinition? = EuPidCredentialMdocClaimDefinitionResolver().resolveOrNull(
        namespace = EuPidScheme.isoNamespace,
        claimName = claimName,
    )

    private fun withSdJwtNames(
        attributeName: NormalizedJsonPath
    ) = EuPidCredentialSdJwtClaimDefinitionResolver().resolveOrNull(attributeName)


    private fun EuPidCredentialClaimDefinition.stringResourceOrNull() = when(this) {
        EuPidCredentialClaimDefinition.FAMILY_NAME -> Res.string.attribute_friendly_name_lastname
        EuPidCredentialClaimDefinition.GIVEN_NAME -> Res.string.attribute_friendly_name_firstname
        EuPidCredentialClaimDefinition.BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
        EuPidCredentialClaimDefinition.PORTRAIT -> Res.string.attribute_friendly_name_portrait
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_12 -> Res.string.attribute_friendly_name_age_at_least_12
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_13 -> Res.string.attribute_friendly_name_age_at_least_13
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_14 -> Res.string.attribute_friendly_name_age_at_least_14
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_16 -> Res.string.attribute_friendly_name_age_at_least_16
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_18 -> Res.string.attribute_friendly_name_age_at_least_18
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_21 -> Res.string.attribute_friendly_name_age_at_least_21
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_25 -> Res.string.attribute_friendly_name_age_at_least_25
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_60 -> Res.string.attribute_friendly_name_age_at_least_60
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_62 -> Res.string.attribute_friendly_name_age_at_least_62
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_65 -> Res.string.attribute_friendly_name_age_at_least_65
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_68 -> Res.string.attribute_friendly_name_age_at_least_68
        EuPidCredentialClaimDefinition.AGE_IN_YEARS -> Res.string.attribute_friendly_name_age_in_years
        EuPidCredentialClaimDefinition.AGE_BIRTH_YEAR -> Res.string.attribute_friendly_name_age_birth_year
        EuPidCredentialClaimDefinition.FAMILY_NAME_BIRTH -> Res.string.attribute_friendly_name_family_name_birth
        EuPidCredentialClaimDefinition.GIVEN_NAME_BIRTH -> Res.string.attribute_friendly_name_given_name_birth
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH -> Res.string.attribute_friendly_name_place_of_birth
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_CONTAINER -> Res.string.attribute_friendly_name_birth_place
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_COUNTRY -> Res.string.attribute_friendly_name_birth_country
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_REGION -> Res.string.attribute_friendly_name_birth_state
        EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_LOCALITY -> Res.string.attribute_friendly_name_birth_city
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_CONTAINER -> Res.string.attribute_friendly_name_main_address
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_FORMATTED -> Res.string.attribute_friendly_name_main_address
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY -> Res.string.attribute_friendly_name_main_residence_country
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION -> Res.string.attribute_friendly_name_main_residence_state
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY -> Res.string.attribute_friendly_name_main_residence_city
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE -> Res.string.attribute_friendly_name_main_residence_postal_code
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET -> Res.string.attribute_friendly_name_main_residence_street
        EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER -> Res.string.attribute_friendly_name_main_residence_house_number
        EuPidCredentialClaimDefinition.SEX -> Res.string.attribute_friendly_name_sex
        EuPidCredentialClaimDefinition.NATIONALITIES -> Res.string.attribute_friendly_name_nationality
        EuPidCredentialClaimDefinition.ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
        EuPidCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        EuPidCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        EuPidCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        EuPidCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        EuPidCredentialClaimDefinition.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
        EuPidCredentialClaimDefinition.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
        EuPidCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_personal_administrative_number
        EuPidCredentialClaimDefinition.EMAIL_ADDRESS -> Res.string.attribute_friendly_name_email_address
        EuPidCredentialClaimDefinition.MOBILE_PHONE_NUMBER -> Res.string.attribute_friendly_name_mobile_phone_number
        EuPidCredentialClaimDefinition.TRUST_ANCHOR -> Res.string.attribute_friendly_name_trust_anchor
        EuPidCredentialClaimDefinition.LOCATION_STATUS -> Res.string.attribute_friendly_name_location_status
        EuPidCredentialClaimDefinition.PORTRAIT_CAPTURE_DATE -> Res.string.attribute_friendly_name_portrait_capture_date
        EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_CONTAINER -> null
    }
}
