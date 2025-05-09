package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.ehic.EhicScheme
import at.asitplus.wallet.ehic.EhicScheme.Attributes
import data.PersonalDataCategory

object EhicCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.Metadata to listOf(
            Attributes.ISSUING_COUNTRY,
            Attributes.SOCIAL_SECURITY_NUMBER,
            Attributes.ISSUING_AUTHORITY_ID,
            Attributes.ISSUING_AUTHORITY_NAME,
            Attributes.DOCUMENT_NUMBER,
            Attributes.ISSUANCE_DATE,
            Attributes.EXPIRY_DATE
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = EhicScheme.claimNames.map {
        NormalizedJsonPath() + it
    },
)