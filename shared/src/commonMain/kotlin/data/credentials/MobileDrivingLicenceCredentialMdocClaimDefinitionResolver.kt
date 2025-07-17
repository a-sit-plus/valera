package data.credentials

import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme

class MobileDrivingLicenceCredentialMdocClaimDefinitionResolver {
    fun resolveOrNull(
        namespace: String,
        claimName: String,
    ): MobileDrivingLicenceCredentialClaimDefinition? = with(MobileDrivingLicenceDataElements) {
        when (namespace) {
            MobileDrivingLicenceScheme.isoNamespace -> when (claimName) {
                FAMILY_NAME -> MobileDrivingLicenceCredentialClaimDefinition.FAMILY_NAME
                GIVEN_NAME -> MobileDrivingLicenceCredentialClaimDefinition.GIVEN_NAME
                BIRTH_DATE -> MobileDrivingLicenceCredentialClaimDefinition.BIRTH_DATE
                ISSUE_DATE -> MobileDrivingLicenceCredentialClaimDefinition.ISSUE_DATE
                EXPIRY_DATE -> MobileDrivingLicenceCredentialClaimDefinition.EXPIRY_DATE
                ISSUING_COUNTRY -> MobileDrivingLicenceCredentialClaimDefinition.ISSUING_COUNTRY
                ISSUING_AUTHORITY -> MobileDrivingLicenceCredentialClaimDefinition.ISSUING_AUTHORITY
                DOCUMENT_NUMBER -> MobileDrivingLicenceCredentialClaimDefinition.DOCUMENT_NUMBER
                PORTRAIT -> MobileDrivingLicenceCredentialClaimDefinition.PORTRAIT
                DRIVING_PRIVILEGES -> MobileDrivingLicenceCredentialClaimDefinition.DRIVING_PRIVILEGES
                UN_DISTINGUISHING_SIGN -> MobileDrivingLicenceCredentialClaimDefinition.UN_DISTINGUISHING_SIGN
                ADMINISTRATIVE_NUMBER -> MobileDrivingLicenceCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
                SEX -> MobileDrivingLicenceCredentialClaimDefinition.SEX
                HEIGHT -> MobileDrivingLicenceCredentialClaimDefinition.HEIGHT
                WEIGHT -> MobileDrivingLicenceCredentialClaimDefinition.WEIGHT
                EYE_COLOUR -> MobileDrivingLicenceCredentialClaimDefinition.EYE_COLOUR
                HAIR_COLOUR -> MobileDrivingLicenceCredentialClaimDefinition.HAIR_COLOUR
                BIRTH_PLACE -> MobileDrivingLicenceCredentialClaimDefinition.BIRTH_PLACE
                RESIDENT_ADDRESS -> MobileDrivingLicenceCredentialClaimDefinition.RESIDENT_ADDRESS
                PORTRAIT_CAPTURE_DATE -> MobileDrivingLicenceCredentialClaimDefinition.PORTRAIT_CAPTURE_DATE
                AGE_IN_YEARS -> MobileDrivingLicenceCredentialClaimDefinition.AGE_IN_YEARS
                AGE_BIRTH_YEAR -> MobileDrivingLicenceCredentialClaimDefinition.AGE_BIRTH_YEAR
                AGE_OVER_12 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_12
                AGE_OVER_13 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_13
                AGE_OVER_14 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_14
                AGE_OVER_16 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_16
                AGE_OVER_18 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_18
                AGE_OVER_21 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_21
                AGE_OVER_25 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_25
                AGE_OVER_60 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_60
                AGE_OVER_62 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_62
                AGE_OVER_65 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_65
                AGE_OVER_68 -> MobileDrivingLicenceCredentialClaimDefinition.AGE_OVER_68
                ISSUING_JURISDICTION -> MobileDrivingLicenceCredentialClaimDefinition.ISSUING_JURISDICTION
                NATIONALITY -> MobileDrivingLicenceCredentialClaimDefinition.NATIONALITY
                RESIDENT_CITY -> MobileDrivingLicenceCredentialClaimDefinition.RESIDENT_CITY
                RESIDENT_STATE -> MobileDrivingLicenceCredentialClaimDefinition.RESIDENT_STATE
                RESIDENT_POSTAL_CODE -> MobileDrivingLicenceCredentialClaimDefinition.RESIDENT_POSTAL_CODE
                RESIDENT_COUNTRY -> MobileDrivingLicenceCredentialClaimDefinition.RESIDENT_COUNTRY
                FAMILY_NAME_NATIONAL_CHARACTER -> MobileDrivingLicenceCredentialClaimDefinition.FAMILY_NAME_NATIONAL_CHARACTER
                GIVEN_NAME_NATIONAL_CHARACTER -> MobileDrivingLicenceCredentialClaimDefinition.GIVEN_NAME_NATIONAL_CHARACTER
                SIGNATURE_USUAL_MARK -> MobileDrivingLicenceCredentialClaimDefinition.SIGNATURE_USUAL_MARK
                BIOMETRIC_TEMPLATE_FINGER -> MobileDrivingLicenceCredentialClaimDefinition.BIOMETRIC_TEMPLATE_FINGER
                BIOMETRIC_TEMPLATE_FACE -> MobileDrivingLicenceCredentialClaimDefinition.BIOMETRIC_TEMPLATE_FACE
                BIOMETRIC_TEMPLATE_IRIS -> MobileDrivingLicenceCredentialClaimDefinition.BIOMETRIC_TEMPLATE_IRIS
                BIOMETRIC_TEMPLATE_SIGNATURE_SIGN -> MobileDrivingLicenceCredentialClaimDefinition.BIOMETRIC_TEMPLATE_SIGNATURE_SIGN
                else -> null
            }

            else -> null
        }
    }
}