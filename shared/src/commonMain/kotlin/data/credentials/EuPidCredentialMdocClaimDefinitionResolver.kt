package data.credentials

import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EuPidDataElements as Attributes

class EuPidCredentialMdocClaimDefinitionResolver {
    @Suppress("DEPRECATION")
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): EuPidCredentialClaimDefinition? = with(Attributes) {
        when (namespace) {
            EU_PID_DOCTYPE -> when (claimName) {
                FAMILY_NAME -> EuPidCredentialClaimDefinition.FAMILY_NAME
                GIVEN_NAME -> EuPidCredentialClaimDefinition.GIVEN_NAME
                BIRTH_DATE -> EuPidCredentialClaimDefinition.BIRTH_DATE
                PORTRAIT -> EuPidCredentialClaimDefinition.PORTRAIT
                FAMILY_NAME_BIRTH -> EuPidCredentialClaimDefinition.FAMILY_NAME_BIRTH
                GIVEN_NAME_BIRTH -> EuPidCredentialClaimDefinition.GIVEN_NAME_BIRTH
                PLACE_OF_BIRTH -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH
                RESIDENT_ADDRESS -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_CONTAINER
                RESIDENT_COUNTRY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY
                RESIDENT_STATE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION
                RESIDENT_CITY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY
                RESIDENT_POSTAL_CODE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE
                RESIDENT_STREET -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET
                RESIDENT_HOUSE_NUMBER -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER
                SEX -> EuPidCredentialClaimDefinition.SEX
                NATIONALITY -> EuPidCredentialClaimDefinition.NATIONALITIES
                ISSUANCE_DATE -> EuPidCredentialClaimDefinition.ISSUANCE_DATE
                EXPIRY_DATE -> EuPidCredentialClaimDefinition.EXPIRY_DATE
                ISSUING_AUTHORITY -> EuPidCredentialClaimDefinition.ISSUING_AUTHORITY
                DOCUMENT_NUMBER -> EuPidCredentialClaimDefinition.DOCUMENT_NUMBER
                ISSUING_COUNTRY -> EuPidCredentialClaimDefinition.ISSUING_COUNTRY
                ISSUING_JURISDICTION -> EuPidCredentialClaimDefinition.ISSUING_JURISDICTION
                PERSONAL_ADMINISTRATIVE_NUMBER -> EuPidCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER
                EMAIL_ADDRESS -> EuPidCredentialClaimDefinition.EMAIL_ADDRESS
                MOBILE_PHONE_NUMBER -> EuPidCredentialClaimDefinition.MOBILE_PHONE_NUMBER
                TRUST_ANCHOR -> EuPidCredentialClaimDefinition.TRUST_ANCHOR
                LOCATION_STATUS -> EuPidCredentialClaimDefinition.LOCATION_STATUS
                else -> null
            }

            else -> null
        }
    }
}
