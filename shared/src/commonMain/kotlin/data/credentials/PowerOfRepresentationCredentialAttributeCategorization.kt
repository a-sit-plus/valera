package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.por.PowerOfRepresentationDataElements
import data.PersonalDataCategory

object PowerOfRepresentationCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.RepresentationData to listOf(
            PowerOfRepresentationDataElements.LEGAL_PERSON_IDENTIFIER,
            PowerOfRepresentationDataElements.LEGAL_NAME,
            PowerOfRepresentationDataElements.FULL_POWERS,
            PowerOfRepresentationDataElements.E_SERVICE,
            PowerOfRepresentationDataElements.EFFECTIVE_FROM_DATE,
            PowerOfRepresentationDataElements.EFFECTIVE_UNTIL_DATE,
        ).map { NormalizedJsonPath() + it to null },

        PersonalDataCategory.Metadata to listOf(
            PowerOfRepresentationDataElements.ISSUANCE_DATE,
            PowerOfRepresentationDataElements.EXPIRY_DATE,
            PowerOfRepresentationDataElements.ISSUING_COUNTRY,
            PowerOfRepresentationDataElements.ISSUING_AUTHORITY,
            PowerOfRepresentationDataElements.ISSUING_JURISDICTION,
            PowerOfRepresentationDataElements.DOCUMENT_NUMBER,
            PowerOfRepresentationDataElements.ADMINISTRATIVE_NUMBER,
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = PowerOfRepresentationDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)