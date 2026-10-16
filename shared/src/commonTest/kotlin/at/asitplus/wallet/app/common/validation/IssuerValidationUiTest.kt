package at.asitplus.wallet.app.common.validation

import at.asitplus.etsi.relyingParty.WrpCredential
import at.asitplus.etsi.relyingParty.WrpCredentialMeta
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.etsi.relyingParty.WrpPayload
import at.asitplus.etsi.relyingParty.WrpStatus
import at.asitplus.etsi.relyingParty.WrpStatusList
import at.asitplus.openid.IssuerMetadata
import at.asitplus.openid.SupportedCredentialFormat
import at.asitplus.wallet.lib.oidvci.LoadedIssuerMetadata
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CheckOutcome.Failed
import at.asitplus.wallet.lib.validation.CheckOutcome.Passed
import at.asitplus.wallet.lib.validation.IssuerMetadataChecks
import at.asitplus.wallet.lib.validation.IssuerMetadataValidation
import at.asitplus.wallet.lib.validation.TrustValidation
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class IssuerValidationUiTest {
    private val now = Instant.parse("2026-10-13T12:00:00Z")

    @Test
    fun untrustedIssuerIsAWarningWithItsRegistration() {
        val report = report(trust = Failed(IllegalArgumentException("untrusted WRPAC")))
        val issuer = IssuerValidationUi.of(
            LoadedIssuerMetadata.Signed(
                IssuerMetadataValidation(report, untrustedMetadata = metadata(), untrustedRegistration = registration())
            )
        )

        assertTrue(issuer.signed)
        assertFalse(issuer.verified)
        assertFalse(issuer.outcome!!.integrityFailed)
        assertEquals(FindingUi.Untrusted(WRPAC_PROVIDERS), issuer.outcome.headline.single().check.finding)
        assertEquals(listOf(VCT), issuer.registration?.attestations)
        assertEquals("https://registry.example.com", issuer.registration?.registryUri)
        assertEquals(true, issuer.registeredFor(sdJwt(VCT)))
        assertEquals(false, issuer.registeredFor(sdJwt("urn:example:other")))
    }

    @Test
    fun metadataWithWrongSubjectFailIntegrity() {
        val report = report(subject = Failed(IllegalArgumentException("sub is not the credential issuer")))
        val issuer = IssuerValidationUi.of(LoadedIssuerMetadata.Signed(IssuerMetadataValidation(report)))

        assertTrue(issuer.outcome!!.integrityFailed)
        assertNull(issuer.registration)
    }

    @Test
    fun missingRegistrationInformationIsAWarning() {
        val report = report(
            trust = Failed(IllegalArgumentException("The metadata are not signed with an access certificate")),
            registration = Failed(IllegalArgumentException("The signed metadata contains no issuer_info")),
        )
        val issuer = IssuerValidationUi.of(
            LoadedIssuerMetadata.Signed(IssuerMetadataValidation(report, untrustedMetadata = metadata()))
        )

        assertFalse(issuer.outcome!!.accepted)
        assertFalse(issuer.outcome.integrityFailed)
        assertEquals(listOf("trust", "registration"), issuer.outcome.headline.map { it.check.name })
        assertNull(issuer.registration)
    }

    @Test
    fun registrationIsWhatTheRegistrationCertificateSays() {
        val report = report(registration = Failed(IllegalArgumentException("untrusted WRPRC")))
        val issuer = IssuerValidationUi.of(
            LoadedIssuerMetadata.Signed(
                IssuerMetadataValidation(report, untrustedMetadata = metadata(), untrustedRegistration = registration())
            )
        )

        assertFalse(issuer.verified)
        assertFalse(issuer.outcome!!.integrityFailed)
        assertEquals("NiScy PID Issuer", issuer.registration?.name)
        assertEquals(listOf("NTREU-123456789"), issuer.registration?.identifiers)
        assertEquals(listOf(WrpLangString(lang = "en", value = "PID issuance")), issuer.registration?.description)
        assertEquals(true, issuer.registeredFor(sdJwt(VCT)))
    }

    @Test
    fun unsignedMetadataCanNotBeVerified() {
        val issuer = IssuerValidationUi.of(LoadedIssuerMetadata.Unsigned(metadata()))

        assertFalse(issuer.signed)
        assertFalse(issuer.verified)
        assertNull(issuer.registration)
        assertNull(issuer.registeredFor(sdJwt(VCT)))
    }

    private fun report(
        trust: CheckOutcome = Passed,
        subject: CheckOutcome = Passed,
        registration: CheckOutcome = Passed,
    ) =
        ValidationReport.issuerMetadata(
            checks = IssuerMetadataChecks(
                parsing = Passed,
                signature = Passed,
                trust = TrustValidation(trust, source = WRPAC_PROVIDERS),
                timeliness = Passed,
                subject = subject,
                registration = registration,
            ),
            policy = ValidationPolicies.strict,
            evaluatedAt = now,
        )

    private fun metadata() = IssuerMetadata(
        credentialIssuer = ISSUER,
        credentialEndpointUrl = "$ISSUER/credential",
        supportedCredentialConfigurations = mapOf("pid" to sdJwt(VCT)),
    )

    private fun registration() = WrpPayload(
        name = "NiScy PID Issuer",
        subjectIdentifier = "NTREU-123456789",
        country = "EU",
        registryUri = "https://registry.example.com",
        srvDescription = listOf(listOf(WrpLangString(lang = "en", value = "PID issuance"))),
        entitlements = emptyList(),
        privacyPolicy = "$ISSUER/privacy",
        infoUri = ISSUER,
        certificatePolicy = "https://registrar.example.com/policy",
        iat = now,
        status = WrpStatus(WrpStatusList(idx = 0u, uri = "https://registrar.example.com/status")),
        providesAttestations = listOf(
            WrpCredential(format = "dc+sd-jwt", meta = WrpCredentialMeta(vctValues = listOf(VCT)))
        ),
    )

    private fun sdJwt(vct: String) = SupportedCredentialFormat.forSdJwt(
        scope = vct,
        sdJwtVcType = vct,
        sdJwtClaims = emptySet(),
    )

    private companion object {
        const val ISSUER = "https://issuer.example.com"
        const val VCT = "urn:eudi:pid:1"
        const val WRPAC_PROVIDERS = "WRPAC providers"
    }
}
