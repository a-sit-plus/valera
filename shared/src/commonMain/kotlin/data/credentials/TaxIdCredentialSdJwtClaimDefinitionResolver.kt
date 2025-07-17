package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.taxid.TaxId2025Scheme
import at.asitplus.wallet.taxid.TaxIdScheme

class TaxIdCredentialSdJwtClaimDefinitionResolver {
    fun resolveOrNull(attributeName: NormalizedJsonPath): TaxIdCredentialClaimDefinition? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                TaxIdScheme.Attributes.TAX_NUMBER -> TaxIdCredentialClaimDefinition.TAX_NUMBER
                TaxIdScheme.Attributes.AFFILIATION_COUNTRY -> TaxIdCredentialClaimDefinition.AFFILIATION_COUNTRY
                TaxIdScheme.Attributes.REGISTERED_FAMILY_NAME -> TaxIdCredentialClaimDefinition.REGISTERED_FAMILY_NAME
                TaxIdScheme.Attributes.REGISTERED_GIVEN_NAME -> TaxIdCredentialClaimDefinition.REGISTERED_GIVEN_NAME
                TaxIdScheme.Attributes.RESIDENT_ADDRESS -> TaxIdCredentialClaimDefinition.RESIDENT_ADDRESS
                TaxIdScheme.Attributes.BIRTH_DATE -> TaxIdCredentialClaimDefinition.BIRTH_DATE
                TaxIdScheme.Attributes.CHURCH_TAX_ID -> TaxIdCredentialClaimDefinition.CHURCH_TAX_ID
                TaxIdScheme.Attributes.IBAN -> TaxIdCredentialClaimDefinition.IBAN
                TaxIdScheme.Attributes.PID_ID -> TaxIdCredentialClaimDefinition.PID_ID
                TaxIdScheme.Attributes.ISSUANCE_DATE -> TaxIdCredentialClaimDefinition.ISSUANCE_DATE
                TaxIdScheme.Attributes.VERIFICATION_STATUS -> TaxIdCredentialClaimDefinition.VERIFICATION_STATUS
                TaxIdScheme.Attributes.EXPIRY_DATE -> TaxIdCredentialClaimDefinition.EXPIRY_DATE
                TaxIdScheme.Attributes.ISSUING_AUTHORITY -> TaxIdCredentialClaimDefinition.ISSUING_AUTHORITY
                TaxIdScheme.Attributes.DOCUMENT_NUMBER -> TaxIdCredentialClaimDefinition.DOCUMENT_NUMBER
                TaxIdScheme.Attributes.ADMINISTRATIVE_NUMBER -> TaxIdCredentialClaimDefinition.ADMINISTRATIVE_NUMBER
                TaxIdScheme.Attributes.ISSUING_COUNTRY -> TaxIdCredentialClaimDefinition.ISSUING_COUNTRY
                TaxIdScheme.Attributes.ISSUING_JURISDICTION -> TaxIdCredentialClaimDefinition.ISSUING_JURISDICTION
                else -> null
            }

            else -> null
        }
}