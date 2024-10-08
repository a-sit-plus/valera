package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.eprescription.EPrescriptionDataElements
import data.PersonalDataCategory

object EPrescriptionCredentialAttributeCategorization : CredentialAttributeCategorization.Template(
    mapOf(
        PersonalDataCategory.Metadata to listOf(
            EPrescriptionDataElements.OTT,
            EPrescriptionDataElements.COUNTRY_CODE,
            EPrescriptionDataElements.VALID_UNTIL
        ).map { NormalizedJsonPath() + it to null },
    ),
    allAttributes = EPrescriptionDataElements.ALL_ELEMENTS.map {
        NormalizedJsonPath() + it
    },
)