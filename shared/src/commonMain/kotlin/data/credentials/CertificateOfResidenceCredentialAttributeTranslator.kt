package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_administrative_number
import at.asitplus.valera.resources.attribute_friendly_name_arrival_date
import at.asitplus.valera.resources.attribute_friendly_name_birth_place
import at.asitplus.valera.resources.attribute_friendly_name_date_of_birth
import at.asitplus.valera.resources.attribute_friendly_name_document_number
import at.asitplus.valera.resources.attribute_friendly_name_expiry_date
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_issue_date
import at.asitplus.valera.resources.attribute_friendly_name_issuing_authority
import at.asitplus.valera.resources.attribute_friendly_name_issuing_country
import at.asitplus.valera.resources.attribute_friendly_name_issuing_jurisdiction
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_nationality
import at.asitplus.valera.resources.attribute_friendly_name_residence_address
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_admin_unit_L1
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_admin_unit_L2
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_full_address
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_locator_designator
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_locator_name
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_po_box
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_post_code
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_post_name
import at.asitplus.valera.resources.attribute_friendly_name_residence_address_thoroughfare
import at.asitplus.valera.resources.attribute_friendly_name_sex

object CertificateOfResidenceCredentialAttributeTranslator : CredentialAttributeTranslator {
    fun stringResourceOf(
        claimDefinition: CertificateOfResidenceCredentialClaimDefinition
    ) = claimDefinition.stringResource()

    private fun CertificateOfResidenceCredentialClaimDefinition.stringResource() = when (this) {
        CertificateOfResidenceCredentialClaimDefinition.ADMINISTRATIVE_NUMBER -> Res.string.attribute_friendly_name_administrative_number
        CertificateOfResidenceCredentialClaimDefinition.ISSUANCE_DATE -> Res.string.attribute_friendly_name_issue_date
        CertificateOfResidenceCredentialClaimDefinition.EXPIRY_DATE -> Res.string.attribute_friendly_name_expiry_date
        CertificateOfResidenceCredentialClaimDefinition.ISSUING_AUTHORITY -> Res.string.attribute_friendly_name_issuing_authority
        CertificateOfResidenceCredentialClaimDefinition.DOCUMENT_NUMBER -> Res.string.attribute_friendly_name_document_number
        CertificateOfResidenceCredentialClaimDefinition.ISSUING_COUNTRY -> Res.string.attribute_friendly_name_issuing_country
        CertificateOfResidenceCredentialClaimDefinition.ISSUING_JURISDICTION -> Res.string.attribute_friendly_name_issuing_jurisdiction
        CertificateOfResidenceCredentialClaimDefinition.FAMILY_NAME -> Res.string.attribute_friendly_name_firstname
        CertificateOfResidenceCredentialClaimDefinition.GIVEN_NAME -> Res.string.attribute_friendly_name_lastname
        CertificateOfResidenceCredentialClaimDefinition.BIRTH_DATE -> Res.string.attribute_friendly_name_date_of_birth
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS -> Res.string.attribute_friendly_name_residence_address
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_PO_BOX -> Res.string.attribute_friendly_name_residence_address_po_box
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_THOROUGHFARE -> Res.string.attribute_friendly_name_residence_address_thoroughfare
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_DESIGNATOR -> Res.string.attribute_friendly_name_residence_address_locator_designator
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_LOCATOR_NAME -> Res.string.attribute_friendly_name_residence_address_locator_name
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_CODE -> Res.string.attribute_friendly_name_residence_address_post_code
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_POST_NAME -> Res.string.attribute_friendly_name_residence_address_post_name
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_1 -> Res.string.attribute_friendly_name_residence_address_admin_unit_L1
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_ADMIN_UNIT_L_2 -> Res.string.attribute_friendly_name_residence_address_admin_unit_L2
        CertificateOfResidenceCredentialClaimDefinition.RESIDENCE_ADDRESS_FULL_ADDRESS -> Res.string.attribute_friendly_name_residence_address_full_address
        CertificateOfResidenceCredentialClaimDefinition.GENDER -> Res.string.attribute_friendly_name_sex
        CertificateOfResidenceCredentialClaimDefinition.BIRTH_PLACE -> Res.string.attribute_friendly_name_birth_place
        CertificateOfResidenceCredentialClaimDefinition.ARRIVAL_DATE -> Res.string.attribute_friendly_name_arrival_date
        CertificateOfResidenceCredentialClaimDefinition.NATIONALITY -> Res.string.attribute_friendly_name_nationality
    }

    override fun translate(
        attributeName: NormalizedJsonPath,
    ) = CertificateOfResidenceCredentialClaimDefinitionResolver().resolveOrNull(
        SdJwtClaimReference(attributeName)
    )?.stringResource()

    fun translate(claimReference: SingleClaimReference) = when (claimReference) {
        is MdocClaimReference -> CertificateOfResidenceCredentialMdocClaimDefinitionResolver().resolveOrNull(
            namespace = claimReference.namespace,
            claimName = claimReference.claimName,
        )

        is SdJwtClaimReference -> CertificateOfResidenceCredentialSdJwtClaimDefinitionResolver().resolveOrNull(
            claimReference.normalizedJsonPath
        )
    }?.stringResource()
}
