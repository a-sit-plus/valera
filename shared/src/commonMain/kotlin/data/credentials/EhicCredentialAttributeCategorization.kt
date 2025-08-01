package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes
import data.PersonalDataCategory

@Suppress("DEPRECATION")
object EhicCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.Metadata to listOf(
            Attributes.ISSUING_COUNTRY,
            Attributes.SOCIAL_SECURITY_NUMBER,
            Attributes.PERSONAL_ADMINISTRATIVE_NUMBER,
            Attributes.ISSUING_AUTHORITY_ID,
            Attributes.ISSUING_AUTHORITY_NAME,
            Attributes.AUTHENTIC_SOURCE_ID,
            Attributes.AUTHENTIC_SOURCE_NAME,
            Attributes.DOCUMENT_NUMBER,
            Attributes.ISSUANCE_DATE,
            Attributes.DATE_OF_ISSUANCE,
            Attributes.EXPIRY_DATE,
            Attributes.DATE_OF_EXPIRY,
            Attributes.STARTING_DATE,
            Attributes.ENDING_DATE,
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = EhicScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)