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
                PREFIX_AGE_EQUAL_OR_OVER -> when (val second = path.segments.getOrNull(1)) {
                    is NormalizedJsonPathSegment.NameSegment -> when (second.memberName) {
                        AgeEqualOrOver.EQUAL_OR_OVER_12 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_12
                        AgeEqualOrOver.EQUAL_OR_OVER_13 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_13
                        AgeEqualOrOver.EQUAL_OR_OVER_14 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_14
                        AgeEqualOrOver.EQUAL_OR_OVER_16 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_16
                        AgeEqualOrOver.EQUAL_OR_OVER_18 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_18
                        AgeEqualOrOver.EQUAL_OR_OVER_21 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_21
                        AgeEqualOrOver.EQUAL_OR_OVER_25 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_25
                        AgeEqualOrOver.EQUAL_OR_OVER_60 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_60
                        AgeEqualOrOver.EQUAL_OR_OVER_62 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_62
                        AgeEqualOrOver.EQUAL_OR_OVER_65 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_65
                        AgeEqualOrOver.EQUAL_OR_OVER_68 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_68
                        else -> null
                    }
                    null -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_CONTAINER
                    else -> null
                }
                // TODO: are those deprecated? accessing without age prefix seems weird
                AGE_EQUAL_OR_OVER_12 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_12
                AGE_EQUAL_OR_OVER_13 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_13
                AGE_EQUAL_OR_OVER_14 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_14
                AGE_EQUAL_OR_OVER_16 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_16
                AGE_EQUAL_OR_OVER_18 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_18
                AGE_EQUAL_OR_OVER_21 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_21
                AGE_EQUAL_OR_OVER_25 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_25
                AGE_EQUAL_OR_OVER_60 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_60
                AGE_EQUAL_OR_OVER_62 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_62
                AGE_EQUAL_OR_OVER_65 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_65
                AGE_EQUAL_OR_OVER_68 -> EuPidCredentialClaimDefinition.AGE_EQUAL_OR_OVER_68
                AGE_IN_YEARS -> EuPidCredentialClaimDefinition.AGE_IN_YEARS
                AGE_BIRTH_YEAR -> EuPidCredentialClaimDefinition.AGE_BIRTH_YEAR
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

