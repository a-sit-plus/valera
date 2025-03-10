package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.taxid.TaxIdScheme
import data.PersonalDataCategory

object TaxIdCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.IdentityData to listOf(
            TaxIdScheme.Attributes.REGISTERED_FAMILY_NAME,
            TaxIdScheme.Attributes.REGISTERED_GIVEN_NAME,
            TaxIdScheme.Attributes.BIRTH_DATE,
            ).map { NormalizedJsonPath() + it to null },
        PersonalDataCategory.ResidenceData to listOf(
            TaxIdScheme.Attributes.RESIDENT_ADDRESS,
        ).map { NormalizedJsonPath() + it to null },
        PersonalDataCategory.Metadata to listOf(
            TaxIdScheme.Attributes.TAX_NUMBER,
            TaxIdScheme.Attributes.AFFILIATION_COUNTRY,
            TaxIdScheme.Attributes.CHURCH_TAX_ID,
            TaxIdScheme.Attributes.IBAN,
            TaxIdScheme.Attributes.PID_ID,
            TaxIdScheme.Attributes.ISSUANCE_DATE,
            TaxIdScheme.Attributes.VERIFICATION_STATUS,
            TaxIdScheme.Attributes.EXPIRY_DATE,
            TaxIdScheme.Attributes.ISSUING_AUTHORITY,
            TaxIdScheme.Attributes.DOCUMENT_NUMBER,
            TaxIdScheme.Attributes.ADMINISTRATIVE_NUMBER,
            TaxIdScheme.Attributes.ISSUING_COUNTRY,
            TaxIdScheme.Attributes.ISSUING_JURISDICTION,
            ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = TaxIdScheme.claimNames.map {
        NormalizedJsonPath(NameSegment(it))
    },
)