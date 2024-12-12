package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.OTT
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.VALID_UNTIL
import at.asitplus.wallet.eprescription.EPrescriptionDataElements.COUNTRY_CODE
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_country_code
import at.asitplus.valera.resources.attribute_friendly_name_one_time_token
import at.asitplus.valera.resources.attribute_friendly_name_valid_until
import org.jetbrains.compose.resources.StringResource


object EPrescriptionCredentialAttributeTranslator : CredentialAttributeTranslator {
    override fun translate(attributeName: NormalizedJsonPath): StringResource? =
        when (val first = attributeName.segments.firstOrNull()) {
            is NormalizedJsonPathSegment.NameSegment -> when (first.memberName) {
                OTT -> Res.string.attribute_friendly_name_one_time_token
                COUNTRY_CODE -> Res.string.attribute_friendly_name_country_code
                VALID_UNTIL -> Res.string.attribute_friendly_name_valid_until
                else -> null
            }

            else -> null
        }
}