package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.etsi.relyingParty.WrpPayload
import at.asitplus.etsi.relyingParty.WrpStatus
import at.asitplus.etsi.relyingParty.WrpStatusList
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialQuery
import at.asitplus.wallet.app.common.validation.UnregisteredRequestUi
import at.asitplus.wallet.app.common.validation.ValidationReportFixtures
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.agent.validation.relyingParty.MissingRegistrationCertificateException
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.RequestDataValidity
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.data.MdocClaimReference
import at.asitplus.wallet.lib.validation.CheckOutcome.Failed
import at.asitplus.wallet.lib.validation.CheckOutcome.NotApplicable
import at.asitplus.wallet.lib.validation.RelyingPartyValidationResult
import at.asitplus.wallet.lib.validation.UnregisteredRequestException
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

internal class RelyingPartyValidationUiTest : ValidationReportFixtures() {
    @Test
    fun registeredRelyingParty() {
        val validation = validation(request(relyingParty = relyingParty()))

        assertFalse(validation.registrationMissing)
        assertTrue(validation.registrationVerified)
        assertFalse(validation.unsigned)
        assertEquals(emptyList(), validation.unregisteredRequests)
    }

    @Test
    fun requestWithoutRegistrationCertificate() {
        val validation = validation(request())

        assertTrue(validation.registrationMissing)
        assertFalse(validation.registrationVerified)
    }

    @Test
    fun unsignedRequest() {
        val validation = validation(request(signature = NotApplicable))

        assertTrue(validation.unsigned)
        assertTrue(validation.outcome!!.accepted)
    }

    @Test
    fun overAskingRelyingParty() {
        val request = WrpCredentialRequest.WrpDcqlCredentialQuery(
            DCQLIsoMdocCredentialQuery(
                id = DCQLCredentialQueryIdentifier("pid"),
                meta = DCQLIsoMdocCredentialMetadataAndValidityConstraints(doctypeValue = DOC_TYPE),
            )
        )
        val validity = RequestDataValidity(
            credentialTypeValidity = true,
            credentialAttributesValidity = listOf(MdocClaimReference(DOC_TYPE, "given_name") to false),
        )
        val validation = validation(
            request(
                relyingParty = relyingParty(
                    registrationCertificate = registrationCertificate(
                        requestAuthorization = Failed(UnregisteredRequestException(listOf(request to validity)))
                    )
                )
            )
        )

        assertFalse(validation.registrationVerified)
        assertFalse(validation.outcome!!.integrityFailed)
        assertEquals(listOf(UnregisteredRequestUi(request, validity)), validation.unregisteredRequests)
    }

    @Test
    fun rejectedReaderOfProximityRequest() {
        val report = relyingParty(accessCertificate(trust = Failed(IllegalArgumentException("untrusted reader"))))
        val validation = RelyingPartyValidationUi.of(RelyingPartyValidationResult(report, relyingParty = null))

        assertFalse(validation.outcome!!.accepted)
        assertFalse(validation.outcome.integrityFailed)
        assertNull(validation.registration)
        assertFalse(validation.registrationMissing)
    }

    @Test
    fun registrationOfRejectedRelyingPartyIsShownUnverified() {
        val report = relyingParty(
            registrationCertificate = registrationCertificate(linkage = Failed(IllegalArgumentException("not linked")))
        )
        val payload = WrpPayload(
            name = "Example Shop",
            subjectIdentifier = "ATU12345678",
            country = "AT",
            registryUri = "https://registry.example.com",
            srvDescription = emptyList(),
            entitlements = emptyList(),
            privacyPolicy = "https://shop.example.com/privacy",
            infoUri = "https://shop.example.com",
            certificatePolicy = "https://registrar.example.com/policy",
            iat = Instant.parse("2026-10-01T00:00:00Z"),
            status = WrpStatus(WrpStatusList(idx = 0u, uri = "https://registrar.example.com/status")),
            purpose = listOf(WrpLangString(lang = "en", value = "Age verification")),
        )
        val validation = RelyingPartyValidationUi.of(
            RelyingPartyValidationResult(report, relyingParty = null, untrustedRegistrations = listOf(payload))
        )

        assertEquals("Example Shop", validation.registration?.name)
        assertEquals(payload.purpose, validation.registration?.purpose)
        assertFalse(validation.registrationVerified)
    }

    @Test
    fun readerWithoutRegistrationCertificate() {
        val report = relyingParty(
            registrationCertificate = null,
            registration = Failed(MissingRegistrationCertificateException("No DocRequest contains a WRPRC")),
        )
        val validation = RelyingPartyValidationUi.of(RelyingPartyValidationResult(report, relyingParty = null))

        assertFalse(validation.outcome!!.integrityFailed)
        assertTrue(validation.registrationMissing)
        assertNull(validation.registration)
    }

    private fun validation(report: ValidationReport) =
        RelyingPartyValidationUi(outcome = report.toUi(), registration = null)

    private companion object {
        const val DOC_TYPE = "eu.europa.ec.eudi.pid.1"
    }
}
