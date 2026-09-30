package at.asitplus.wallet.app.common.relyingParty

import at.asitplus.etsi.relyingParty.WrpPayload
import at.asitplus.etsi.relyingParty.WrpStatus
import at.asitplus.etsi.relyingParty.WrpStatusList
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpRegistrationCertificate
import at.asitplus.wallet.lib.agent.validation.relyingParty.WrpValidationException
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacIdentifier
import at.asitplus.wallet.lib.agent.validation.relyingParty.accessCertificate.WrpacValidationResult
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpRegistrationCertificateValidation
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrprcValidationResult
import at.asitplus.wallet.lib.jws.JwsHeaderNone
import at.asitplus.wallet.lib.jws.SignJwt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class WrpValidationOutcomeTest {

    @Test
    fun missingRegistrationCertificateIsNotProvided() {
        assertEquals(
            WrpValidationFailure(WrpValidationFailure.Reason.NOT_PROVIDED),
            WrpValidationException.RegistrationCertificateMissing("No verifier_info in request").toWrpValidationFailure(),
        )
    }

    @Test
    fun unparsableRegistrationCertificateIsMalformedWithTheReason() {
        val failure = WrpValidationException.RegistrationCertificateMalformed(
            "Registration certificate could not be parsed: Field 'status' is required"
        ).toWrpValidationFailure()

        assertEquals(WrpValidationFailure.Reason.MALFORMED, failure.reason)
        assertTrue(failure.detail!!.contains("status"))
    }

    @Test
    fun unsupportedRequestIsUnsupported() {
        assertEquals(
            WrpValidationFailure.Reason.UNSUPPORTED,
            WrpValidationException.UnsupportedRequest("Request not supported").toWrpValidationFailure().reason,
        )
    }

    @Test
    fun otherFailuresAreReportedAsMalformedWithTheirMessage() {
        val failure = IllegalStateException("No DCQL query in request").toWrpValidationFailure()

        assertEquals(WrpValidationFailure(WrpValidationFailure.Reason.MALFORMED, "No DCQL query in request"), failure)
    }

    @Test
    fun identifierMismatchIsReportedApartFromTheCertificate() = runTest {
        val result = WrpValidationResult(
            wrprcValidationResult(validation(validLinkage = false)),
            wrpacValidationResult(),
        )

        assertTrue(result.validCertificate)
        assertFalse(result.validIdentifier)
        assertFalse(result.statusListUnresolved)
    }

    @Test
    fun unreachableStatusListIsReportedApartFromARevokedStatus() = runTest {
        val unreachable = WrpValidationResult(
            wrprcValidationResult(validation(validStatusList = false, statusListResolved = false)),
            wrpacValidationResult(),
        )
        val revoked = WrpValidationResult(
            wrprcValidationResult(validation(validStatusList = false, statusListResolved = true)),
            wrpacValidationResult(),
        )

        assertFalse(unreachable.validCertificate)
        assertTrue(unreachable.statusListUnresolved)
        assertFalse(revoked.validCertificate)
        assertFalse(revoked.statusListUnresolved)
    }

    @Test
    fun untrustedAccessCertificateDoesNotInvalidateTheRegistrationCertificate() = runTest {
        val result = WrpValidationResult(
            wrprcValidationResult(validation()),
            wrpacValidationResult(validChain = false),
        )

        assertTrue(result.validCertificate)
        assertTrue(result.validIdentifier)
    }

    private fun validation(
        validLinkage: Boolean = true,
        validStatusList: Boolean = true,
        statusListResolved: Boolean = true,
    ) = WrpRegistrationCertificateValidation(
        validHeader = true,
        validSignature = true,
        validChain = true,
        validPayload = true,
        validLinkage = validLinkage,
        validStatusList = validStatusList,
        statusListResolved = statusListResolved,
    )

    private suspend fun wrprcValidationResult(validation: WrpRegistrationCertificateValidation) = WrprcValidationResult(
        certificateValidation = mapOf(registrationCertificate() to validation),
        requestDataValidation = emptyList(),
    )

    private fun wrpacValidationResult(validChain: Boolean = true) = WrpacValidationResult(
        chain = emptyList(),
        identifierResult = WrpacIdentifier.WrpacLegalIdentifier(WRP_ID),
        validLinkage = true,
        validChain = validChain,
    )

    /** Only needed as a key: these tests map validation results that are given, not validate the certificate. */
    private suspend fun registrationCertificate(): WrpRegistrationCertificate {
        val payload = WrpPayload(
            subjectIdentifier = WRP_ID,
            country = "AT",
            registryUri = "https://registrar.example.invalid/wrp",
            srvDescription = emptyList(),
            entitlements = emptyList(),
            privacyPolicy = "https://relying-party.example.invalid/privacy",
            infoUri = "",
            certificatePolicy = "https://registrar.example.invalid/policy",
            iat = Instant.fromEpochSeconds(0),
            status = WrpStatus(WrpStatusList(0u, "https://registrar.example.invalid/status")),
        )
        val jws = SignJwt<WrpPayload>(EphemeralKeyWithoutCert(), JwsHeaderNone())(
            type = "rc-wrp+jwt",
            payload = payload,
            serializer = WrpPayload.serializer(),
        ).getOrThrow()
        return WrpRegistrationCertificate.WrpJwtRegistrationCertificate(jwsTyped = jws)
    }

    private companion object {
        const val WRP_ID = "WRP-TEST00000001"
    }
}
