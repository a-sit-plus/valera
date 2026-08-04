package data.credentials

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.ClaimDescription
import at.asitplus.openid.DisplayProperties
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.jwt_claim_label_exp
import at.asitplus.valera.resources.jwt_claim_label_iss
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupid.EuPidDataElements as EuPidMdocElements
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.eupidsdjwt.EuPidSdJwtDataElements as EuPidSdJwtElements
import at.asitplus.wallet.lib.data.ExtractedIsoMdocCredentialScheme
import at.asitplus.wallet.lib.data.ExtractedSdJwtCredentialScheme
import at.asitplus.wallet.mdl.MDL_DOCTYPE
import at.asitplus.wallet.mdl.MDL_NAMESPACE
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataCredentialRenderingTest {
    @Test
    fun mapsMdlClaimLabelsFromMetadata() {
        val scheme = mdlScheme()

        assertEquals(
            "Family name",
            scheme.getLocalization(
                MdocClaimReference(MDL_NAMESPACE, MobileDrivingLicenceDataElements.FAMILY_NAME)
            )
        )
        assertEquals(
            "Family name",
            scheme.metadataLabel(NormalizedJsonPath() + MobileDrivingLicenceDataElements.FAMILY_NAME)
        )
        assertNull(
            scheme.getLocalization(
                MdocClaimReference("other", MobileDrivingLicenceDataElements.FAMILY_NAME)
            )
        )
    }

    @Test
    fun mapsEuPidMdocClaimLabelsFromMetadata() {
        val scheme = euPidIsoScheme()

        assertEquals(
            "Given name",
            scheme.getLocalization(
                MdocClaimReference(EU_PID_DOCTYPE, EuPidMdocElements.GIVEN_NAME)
            )
        )
        assertEquals("Given name", scheme.metadataLabel(NormalizedJsonPath() + EuPidMdocElements.GIVEN_NAME))
        assertNull(
            scheme.getLocalization(
                MdocClaimReference("other", EuPidMdocElements.GIVEN_NAME)
            )
        )
    }

    @Test
    fun mapsEuPidSdJwtNestedAndFlatClaimLabelsFromMetadata() {
        val scheme = euPidSdJwtScheme()

        assertEquals(
            "Resident country",
            scheme.getLocalization(
                JsonClaimReference(
                    NormalizedJsonPath() + EuPidSdJwtElements.PREFIX_ADDRESS + EuPidSdJwtElements.Address.COUNTRY
                )
            )
        )
        assertEquals(
            "Resident country",
            scheme.getLocalization(
                JsonClaimReference(NormalizedJsonPath() + EuPidSdJwtElements.ADDRESS_COUNTRY)
            )
        )
        assertEquals(
            "Birth country",
            scheme.getLocalization(
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

private fun mdlScheme() = ExtractedIsoMdocCredentialScheme(
    isoDocType = MDL_DOCTYPE,
    isoNamespace = MDL_NAMESPACE,
    claimDescriptions = setOf(
        claimDescription("Family name", MDL_NAMESPACE, MobileDrivingLicenceDataElements.FAMILY_NAME)
    ),
)

private fun euPidIsoScheme() = ExtractedIsoMdocCredentialScheme(
    isoDocType = EU_PID_DOCTYPE,
    isoNamespace = EU_PID_DOCTYPE,
    claimDescriptions = setOf(
        claimDescription("Given name", EU_PID_DOCTYPE, EuPidMdocElements.GIVEN_NAME)
    ),
)

private fun euPidSdJwtScheme() = ExtractedSdJwtCredentialScheme(
    sdJwtType = EU_PID_SD_JWT_VCT,
    claimDescriptions = setOf(
        claimDescription("Resident country", EuPidSdJwtElements.PREFIX_ADDRESS, EuPidSdJwtElements.Address.COUNTRY),
        claimDescription(
            "Birth country",
            EuPidSdJwtElements.PREFIX_PLACE_OF_BIRTH,
            EuPidSdJwtElements.PlaceOfBirth.COUNTRY,
        ),
    ),
)

private fun claimDescription(label: String, vararg path: String) = ClaimDescription(
    path = OpenId4VciClaimsPathPointer(path.toList()),
    display = setOf(DisplayProperties(name = label, locale = "en-US")),
)
