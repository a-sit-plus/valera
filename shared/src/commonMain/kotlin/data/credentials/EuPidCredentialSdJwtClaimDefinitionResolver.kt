package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.Address
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.AgeEqualOrOver
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtScheme.SdJwtAttributes.PlaceOfBirth

class EuPidCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(path: NormalizedJsonPath) = with(EuPidSdJwtScheme.SdJwtAttributes) {
        when (val first = path.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                FAMILY_NAME -> EuPidCredentialClaimDefinition.FAMILY_NAME
                GIVEN_NAME -> EuPidCredentialClaimDefinition.GIVEN_NAME
                BIRTH_DATE -> EuPidCredentialClaimDefinition.BIRTH_DATE
                PORTRAIT -> EuPidCredentialClaimDefinition.PORTRAIT
                FAMILY_NAME_BIRTH -> EuPidCredentialClaimDefinition.FAMILY_NAME_BIRTH
                GIVEN_NAME_BIRTH -> EuPidCredentialClaimDefinition.GIVEN_NAME_BIRTH
                PREFIX_PLACE_OF_BIRTH -> when (val second = path.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        PlaceOfBirth.COUNTRY -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_COUNTRY
                        PlaceOfBirth.REGION -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_REGION
                        PlaceOfBirth.LOCALITY -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_LOCALITY
                        else -> null
                    }
                    null -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_CONTAINER
                    else -> null
                }
                PLACE_OF_BIRTH_COUNTRY -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_COUNTRY
                PLACE_OF_BIRTH_REGION -> EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_REGION
                PLACE_OF_BIRTH_LOCALITY ->  EuPidCredentialClaimDefinition.PLACE_OF_BIRTH_LOCALITY

                PREFIX_ADDRESS -> when (val second = path.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        Address.FORMATTED -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_FORMATTED
                        Address.COUNTRY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY
                        Address.REGION -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION
                        Address.LOCALITY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY
                        Address.POSTAL_CODE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE
                        Address.STREET -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET
                        Address.HOUSE_NUMBER -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER
                        else -> null
                    }
                    null -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_CONTAINER
                    else -> null
                }
                ADDRESS_FORMATTED -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_FORMATTED
                ADDRESS_COUNTRY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_COUNTRY
                ADDRESS_REGION -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_REGION
                ADDRESS_LOCALITY -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_LOCALITY
                ADDRESS_POSTAL_CODE -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_POSTAL_CODE
                ADDRESS_STREET -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_STREET
                ADDRESS_HOUSE_NUMBER -> EuPidCredentialClaimDefinition.RESIDENT_ADDRESS_HOUSE_NUMBER

                SEX -> EuPidCredentialClaimDefinition.SEX
                NATIONALITIES -> EuPidCredentialClaimDefinition.NATIONALITIES
                ISSUANCE_DATE -> EuPidCredentialClaimDefinition.ISSUANCE_DATE
                EXPIRY_DATE -> EuPidCredentialClaimDefinition.EXPIRY_DATE
                ISSUING_AUTHORITY -> EuPidCredentialClaimDefinition.ISSUING_AUTHORITY
                DOCUMENT_NUMBER -> EuPidCredentialClaimDefinition.DOCUMENT_NUMBER
                ISSUING_COUNTRY -> EuPidCredentialClaimDefinition.ISSUING_COUNTRY
                ISSUING_JURISDICTION -> EuPidCredentialClaimDefinition.ISSUING_JURISDICTION
                PERSONAL_ADMINISTRATIVE_NUMBER -> EuPidCredentialClaimDefinition.PERSONAL_ADMINISTRATIVE_NUMBER
                EMAIL -> EuPidCredentialClaimDefinition.EMAIL_ADDRESS
                PHONE_NUMBER -> EuPidCredentialClaimDefinition.MOBILE_PHONE_NUMBER
                TRUST_ANCHOR -> EuPidCredentialClaimDefinition.TRUST_ANCHOR
                else -> null
            }

            else -> null
        }
    }
}

