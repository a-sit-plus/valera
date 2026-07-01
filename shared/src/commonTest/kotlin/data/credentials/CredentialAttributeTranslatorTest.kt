package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_birth_country
import at.asitplus.valera.resources.attribute_friendly_name_firstname
import at.asitplus.valera.resources.attribute_friendly_name_lastname
import at.asitplus.valera.resources.attribute_friendly_name_main_residence_country
import at.asitplus.valera.resources.jwt_claim_label_exp
import at.asitplus.valera.resources.jwt_claim_label_iss
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EuPidDataElements as EuPidMdocElements
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements as EuPidSdJwtElements
import at.asitplus.wallet.mdl.MDL_NAMESPACE
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CredentialAttributeTranslatorTest {
    @Test
    fun mapsMdlClaimsByName() {
        val translator = MobileDrivingLicenceCredentialAttributeTranslator()

        assertEquals(
            Res.string.attribute_friendly_name_lastname,
            translator.translateSingleClaimReference(
                MdocClaimReference(MDL_NAMESPACE, MobileDrivingLicenceDataElements.FAMILY_NAME)
            )
        )
        assertNull(
            translator.translateSingleClaimReference(
                MdocClaimReference("other", MobileDrivingLicenceDataElements.FAMILY_NAME)
            )
        )
    }

    @Test
    fun mapsEuPidMdocClaimsByName() {
        val translator = EuPidCredentialAttributeTranslator()

        assertEquals(
            Res.string.attribute_friendly_name_firstname,
            translator.translateSingleClaimReference(
                MdocClaimReference(EU_PID_DOCTYPE, EuPidMdocElements.GIVEN_NAME)
            )
        )
        assertNull(
            translator.translateSingleClaimReference(
                MdocClaimReference("other", EuPidMdocElements.GIVEN_NAME)
            )
        )
    }

    @Test
    fun mapsEuPidSdJwtNestedAndFlatClaimsByPath() {
        val translator = EuPidCredentialAttributeTranslator()

        assertEquals(
            Res.string.attribute_friendly_name_main_residence_country,
            translator.translateSingleClaimReference(
                JsonClaimReference(
                    NormalizedJsonPath() + EuPidSdJwtElements.PREFIX_ADDRESS + EuPidSdJwtElements.Address.COUNTRY
                )
            )
        )
        assertEquals(
            Res.string.attribute_friendly_name_main_residence_country,
            translator.translateSingleClaimReference(
                JsonClaimReference(NormalizedJsonPath() + EuPidSdJwtElements.ADDRESS_COUNTRY)
            )
        )
        assertEquals(
            Res.string.attribute_friendly_name_birth_country,
            translator.translateSingleClaimReference(
                JsonClaimReference(
                    NormalizedJsonPath() + EuPidSdJwtElements.PREFIX_PLACE_OF_BIRTH + EuPidSdJwtElements.PlaceOfBirth.COUNTRY
                )
            )
        )
    }

    @Test
    fun mapsJwtMetadataLabelsByClaimName() {
        assertEquals(Res.string.jwt_claim_label_iss, jwtClaimLabel("iss"))
        assertEquals(Res.string.jwt_claim_label_exp, jwtClaimLabel("exp"))
        assertNull(jwtClaimLabel("kid"))
    }
}
