package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.healthid.HealthIdScheme.Attributes
import data.PersonalDataCategory

object HealthIdCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.Metadata to listOf(
            Attributes.HEALTH_INSURANCE_ID,
            Attributes.PATIENT_ID,
            Attributes.TAX_NUMBER,
            Attributes.ONE_TIME_TOKEN,
            Attributes.E_PRESCRIPTION_CODE,
            Attributes.AFFILIATION_COUNTRY,
            Attributes.ISSUE_DATE,
            Attributes.EXPIRY_DATE,
            Attributes.ISSUING_AUTHORITY,
            Attributes.DOCUMENT_NUMBER,
            Attributes.ADMINISTRATIVE_NUMBER,
            Attributes.ISSUING_COUNTRY,
            Attributes.ISSUING_JURISDICTION
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = HealthIdScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)