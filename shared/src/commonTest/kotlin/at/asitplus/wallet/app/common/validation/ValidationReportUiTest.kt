package at.asitplus.wallet.app.common.validation

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.primitives.TokenStatus
import at.asitplus.wallet.lib.validation.CheckOutcome.Blocked
import at.asitplus.wallet.lib.validation.CheckOutcome.Failed
import at.asitplus.wallet.lib.validation.CheckOutcome.Passed
import at.asitplus.wallet.lib.validation.NamedCheck
import at.asitplus.wallet.lib.validation.Requirement.REQUIRED
import at.asitplus.wallet.lib.validation.StatusValidation
import at.asitplus.wallet.lib.validation.TimelinessException
import at.asitplus.wallet.lib.validation.TokenStatusException
import at.asitplus.wallet.lib.validation.TrustValidation
import at.asitplus.wallet.lib.validation.UnregisteredRequestException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

internal class ValidationReportUiTest : ValidationReportFixtures() {
    @Test
    fun acceptedRequestHasNoProblems() {
        val outcome = request(relyingParty = relyingParty()).toUi()

        assertTrue(outcome.accepted)
        assertFalse(outcome.integrityFailed)
        assertEquals(emptyList(), outcome.headline)
        assertEquals(ArtifactKind.REQUEST_OBJECT, outcome.root.kind)
        val relyingParty = outcome.root.children.single()
        assertEquals(ArtifactKind.RELYING_PARTY, relyingParty.kind)
        assertEquals(
            listOf(ArtifactKind.ACCESS_CERTIFICATE, ArtifactKind.REGISTRATION_CERTIFICATE),
            relyingParty.children.map { it.kind },
        )
    }

    @Test
    fun untrustedAccessCertificateIsAWarning() {
        val outcome = request(
            relyingParty = relyingParty(accessCertificate(trust = Failed(IllegalArgumentException("untrusted"))))
        ).toUi()

        assertFalse(outcome.accepted)
        assertFalse(outcome.integrityFailed)
        val headline = outcome.headline.single()
        assertEquals(ArtifactKind.ACCESS_CERTIFICATE, headline.kind)
        assertEquals("trust", headline.check.name)
        assertEquals(CheckState.FAILED, headline.check.state)
        assertEquals(FindingUi.Untrusted(WRPAC_PROVIDERS), headline.check.finding)
    }

    @Test
    fun missingTrustAnchorsAreNotChecked() {
        val outcome = request(relyingParty = relyingParty(accessCertificate(trust = Blocked()))).toUi()

        assertFalse(outcome.integrityFailed)
        val check = outcome.headline.single().check
        assertEquals(CheckState.NOT_CHECKED, check.state)
        assertEquals(FindingUi.TrustNotChecked(WRPAC_PROVIDERS), check.finding)
    }

    @Test
    fun integrityFailuresComeFirst() {
        val outcome = request(
            timeliness = Failed(IllegalArgumentException("expired")),
            walletNonce = Failed(IllegalArgumentException("unknown wallet_nonce")),
        ).toUi()

        assertTrue(outcome.integrityFailed)
        assertEquals(listOf("walletNonce", "timeliness"), outcome.headline.map { it.check.name })
        assertEquals(FindingUi.Message("unknown wallet_nonce"), outcome.headline.first().check.finding)
    }

    @Test
    fun badSignatureOfAccessCertificateIsAnIntegrityFailure() {
        val outcome = relyingParty(accessCertificate(signature = Failed(IllegalArgumentException("bad signature"))))
            .toUi()

        assertTrue(outcome.integrityFailed)
    }

    @Test
    fun problemsOfRegistrationCertificatesAreWarnings() {
        val outcome = request(
            relyingParty = relyingParty(
                registrationCertificate = registrationCertificate(
                    signature = Failed(IllegalArgumentException("bad signature")),
                    linkage = Failed(IllegalArgumentException("sub is not the WRPAC identifier")),
                    requestAuthorization = Failed(UnregisteredRequestException(emptyList())),
                )
            )
        ).toUi()

        assertFalse(outcome.accepted)
        assertFalse(outcome.integrityFailed)
        val findings = outcome.headline.associate { it.check.name to it.check.finding }
        assertEquals(FindingUi.NotLinked, findings["linkage"])
        assertEquals(FindingUi.Unregistered(emptyList()), findings["requestAuthorization"])
    }

    @Test
    fun revokedRegistrationCertificate() {
        val status = StatusValidation(
            claim = Passed,
            mechanisms = listOf(
                mechanism(
                    token = statusListToken(),
                    outcome = Failed(TokenStatusException(TokenStatus.Invalid)),
                    observed = TokenStatus.Invalid,
                )
            ),
            agreement = Passed,
        )
        val outcome = relyingParty(registrationCertificate = registrationCertificate(status = status)).toUi()

        val registration = outcome.root.children.single { it.kind == ArtifactKind.REGISTRATION_CERTIFICATE }
        val statusCheck = registration.displayedChecks().single { it.name == "status" }
        assertEquals(CheckState.FAILED, statusCheck.state)
        assertEquals(FindingUi.Revoked, statusCheck.finding)
        assertEquals(listOf(ArtifactKind.STATUS_LIST_TOKEN), registration.children.map { it.kind })
        assertFalse(outcome.integrityFailed)
    }

    @Test
    fun unavailableStatusList() {
        val status = StatusValidation(
            claim = Passed,
            mechanisms = listOf(
                mechanism(
                    token = statusListToken(retrieval = Failed(IllegalStateException("offline"))),
                    outcome = Blocked(),
                    observed = null,
                )
            ),
            agreement = Blocked(),
        )
        val outcome = relyingParty(registrationCertificate = registrationCertificate(status = status)).toUi()

        val finding = outcome.headline.first { it.check.name == "status.mechanisms[0]" }.check.finding
        assertEquals(FindingUi.StatusUnavailable("offline"), finding)
    }

    @Test
    fun expiredAccessCertificate() {
        val notAfter = now - 1.days
        val outcome = relyingParty(
            accessCertificate(
                timeliness = Failed(TimelinessException("expired", now, notBefore = null, notAfter = notAfter))
            )
        ).toUi()

        assertFalse(outcome.integrityFailed)
        assertEquals(FindingUi.Expired(notAfter.toEpochMilliseconds()), outcome.headline.single().check.finding)
    }

    @Test
    fun checksThatDoNotApplyAreNotDisplayed() {
        val outcome = relyingParty().toUi()

        val access = outcome.root.children.first()
        assertTrue(access.checks.any { it.name == "clientId" && it.state == CheckState.NOT_APPLICABLE })
        assertTrue(access.displayedChecks().none { it.name == "clientId" })
    }

    @Test
    fun outcomeSurvivesSerialization() {
        val outcome = request(
            relyingParty = relyingParty(accessCertificate(trust = Failed(IllegalArgumentException("untrusted"))))
        ).toUi()

        val decoded = joseCompliantSerializer.decodeFromString<ValidationOutcomeUi>(
            joseCompliantSerializer.encodeToString(outcome)
        )

        assertEquals(outcome, decoded)
    }

    @Test
    fun integrityRejectionSkipsChecksTheUserMayContinuePast() {
        val nonce = IllegalArgumentException("unknown wallet_nonce")
        val report = request(
            timeliness = Failed(IllegalArgumentException("expired")),
            walletNonce = Failed(nonce),
            relyingParty = relyingParty(accessCertificate(trust = Failed(IllegalArgumentException("untrusted")))),
        )

        val rejection = report.integrityRejection()

        assertEquals(
            "The walletNonce check of the RequestObjectChecks failed: unknown wallet_nonce",
            rejection?.message,
        )
        assertEquals(nonce, rejection?.cause)
        assertNull(request(relyingParty = relyingParty(accessCertificate(trust = Blocked()))).integrityRejection())
    }

    @Test
    fun problemsAreDescribedWithTheirCauses() {
        val report = request(
            relyingParty = relyingParty(accessCertificate(trust = Failed(IllegalArgumentException("untrusted")))),
        )

        assertEquals("AccessCertificateChecks.trust: untrusted", report.describeProblems())
        assertEquals("none", request().describeProblems())
    }

    @Test
    fun integrityRejectionIgnoresRegistrationCertificates() {
        val report = relyingParty(
            registrationCertificate = registrationCertificate(signature = Failed(IllegalArgumentException("bad")))
        )

        assertNull(report.integrityRejection())
    }

    @Test
    fun rejectionIsTheCauseOfTheFirstUnsatisfiedCheck() {
        val cause = IllegalArgumentException("untrusted")
        val report = request(relyingParty = relyingParty(accessCertificate(trust = Failed(cause))))

        assertEquals(cause, report.rejection())
        assertNull(request().rejection())
    }

    @Test
    fun trustTimeStatusAndRegistrationAreTolerable() {
        listOf("trust", "issuerTrust", "signerTrust").forEach {
            assertTrue(NamedCheck(it, Blocked(), REQUIRED, trust = TrustValidation(Blocked())).tolerable(), it)
        }
        listOf("timeliness", "status.claim", "status.mechanisms[0]", "status.agreement", "linkage", "requestAuthorization")
            .forEach { assertTrue(NamedCheck(it, Blocked(), REQUIRED).tolerable(), it) }
    }

    @Test
    fun integrityChecksAndUnknownChecksBlock() {
        listOf(
            "parsing", "signature", "issuerSignature", "popSignature", "holderBinding", "clientId", "walletNonce",
            "expectedOrigin", "challenge", "nonce", "audience", "subject", "proofTime", "popTime", "requestData",
            "disclosedItems[0].digest", "semantics", "aCheckVckAddsLater",
        ).forEach { assertFalse(NamedCheck(it, Blocked(), REQUIRED).tolerable(), it) }
    }

    @Test
    fun displayTextJoinsTheMessagesOfCauses() {
        val text = IllegalArgumentException("Invalid WRPRC", IllegalStateException("Invalid JWS")).displayText()

        assertEquals("Invalid WRPRC: Invalid JWS", text)
        assertEquals("IllegalStateException", IllegalStateException().displayText())
    }

}
