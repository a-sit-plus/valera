package ui.viewmodels

import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.SupportedCredentialFormatIsoMdoc
import at.asitplus.openid.SupportedCredentialFormatSdJwt
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialRepresentation
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialType
import at.asitplus.wallet.lib.ktor.openid.CredentialIdentifierInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadCredentialViewModelTest {
    private val issuerMetadata = IssuerMetadata(
        credentialIssuer = "https://issuer.example",
        credentialEndpointUrl = "https://issuer.example/credential",
    )

    @Test
    fun filtersIssuerConfigurationsByRequestedRepresentationAndType() {
        val mdoc = credential("mdoc", SupportedCredentialFormatIsoMdoc("type-a"))
        val matchingSdJwt = credential("sd-a", SupportedCredentialFormatSdJwt("type-a"))
        val otherSdJwt = credential("sd-b", SupportedCredentialFormatSdJwt("type-b"))

        assertEquals(
            listOf(matchingSdJwt),
            listOf(mdoc, matchingSdJwt, otherSdJwt).filterFor(
                DCAPICredentialType(DCAPICredentialRepresentation.SD_JWT, "type-a")
            ),
        )
        assertEquals(
            listOf(mdoc),
            listOf(mdoc, matchingSdJwt, otherSdJwt).filterFor(
                DCAPICredentialType(DCAPICredentialRepresentation.ISO_MDOC, "type-a")
            ),
        )
    }

    private fun credential(identifier: String, format: at.asitplus.openid.SupportedCredentialFormat) =
        CredentialIdentifierInfo(issuerMetadata, identifier, format)
}
