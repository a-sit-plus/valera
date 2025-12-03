package data.credentials

import at.asitplus.wallet.eupid.EuPidScheme

class EuPidCredentialMdocClaimDefinitionResolver {
    @Suppress("DEPRECATION")
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): EuPidCredentialClaimDefinition? = with(EuPidScheme.Attributes) {
        when (namespace) {
            EuPidScheme.isoNamespace -> when (claimName) {
                FAMILY_NAME -> EuPidCredentialClaimDefinition.FAMILY_NAME
                GIVEN_NAME -> EuPidCredentialClaimDefinition.GIVEN_NAME
                BIRTH_DATE -> EuPidCredentialClaimDefinition.BIRTH_DATE
                PORTRAIT -> EuPidCredentialClaimDefinition.PORTRAIT
                AGE_OVER_12 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_12
                AGE_OVER_13 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_13
                AGE_OVER_14 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_14
                AGE_OVER_16 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_16
                AGE_OVER_18 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_18
                AGE_OVER_21 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_21
                AGE_OVER_25 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_25
                AGE_OVER_60 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_60
                AGE_OVER_62 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_62
                AGE_OVER_65 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_65
                AGE_OVER_68 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_68
                AGE_IN_YEARS -> EuPidCredentialClaimDefinition.AGE_IN_YEARS
                AGE_BIRTH_YEAR -> EuPidCredentialClaimDefinition.AGE_BIRTH_YEAR
                FAMILY_NAME_BIRTH -> EuPidCredentialClaimDefinition.FAMILY_NAME_BIRTH
                GIVEN_NAME_BIRTH -> EuPidCredentialClaimDefinition.GIVEN_NAME_BIRTH
                PLACE_OF_BIRTH -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH
                BIRTH_PLACE -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_CONTAINER
                BIRTH_COUNTRY -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_COUNTRY
                BIRTH_STATE -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_REGION
                BIRTH_CITY -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_LOCALITY
                RESIDENT_ADDRESS -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_CONTAINER
                RESIDENT_COUNTRY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY
                RESIDENT_STATE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION
                RESIDENT_CITY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY
                RESIDENT_POSTAL_CODE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE
                RESIDENT_STREET -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET
                RESIDENT_HOUSE_NUMBER -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER
                GENDER -> EuPidCredentialClaimDefinition.SEX
                SEX -> EuPidCredentialClaimDefinition.SEX
                NATIONALITY -> EuPidCredentialClaimDefinition.NATIONALITIES
                ISSUANCE_DATE -> EuPidCredentialClaimDefinition.ISSUANCE_DATE
                EXPIRY_DATE -> EuPidCredentialClaimDefinition.EXPIRY_DATE
                ISSUING_AUTHORITY -> EuPidCredentialClaimDefinition.ISSUING_AUTHORITY
                DOCUMENT_NUMBER -> EuPidCredentialClaimDefinition.DOCUMENT_NUMBER
                ADMINISTRATIVE_NUMBER -> EuPidCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
                ISSUING_COUNTRY -> EuPidCredentialClaimDefinition.ISSUING_COUNTRY
                ISSUING_JURISDICTION -> EuPidCredentialClaimDefinition.ISSUING_JURISDICTION
                PERSONAL_ADMINISTRATIVE_NUMBER -> EuPidCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER
                EMAIL_ADDRESS -> EuPidCredentialClaimDefinition.EMAIL_ADDRESS
                MOBILE_PHONE_NUMBER -> EuPidCredentialClaimDefinition.MOBILE_PHONE_NUMBER
                TRUST_ANCHOR -> EuPidCredentialClaimDefinition.TRUST_ANCHOR
                LOCATION_STATUS -> EuPidCredentialClaimDefinition.LOCATION_STATUS
                PORTRAIT_CAPTURE_DATE -> EuPidCredentialClaimDefinition.PORTRAIT_CAPTURE_DATE
                else -> null
            }

            else -> null
        }
    }
}