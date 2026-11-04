package at.asitplus.wallet.app.common.iso.verifier

import at.asitplus.iso.DeviceAuth
import at.asitplus.iso.DeviceNameSpaces
import at.asitplus.iso.DeviceResponse
import at.asitplus.iso.DeviceSigned
import at.asitplus.iso.Document
import at.asitplus.iso.IssuerSignedItem
import at.asitplus.iso.SessionTranscript
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.supreme.agree.keyAgreement
import at.asitplus.wallet.app.common.iso.transfer.encodeSessionTranscript
import at.asitplus.wallet.app.common.iso.transfer.toKeyAgreementPrivateValue
import at.asitplus.wallet.app.common.validation.toUi
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.IsoDeviceSignatureInput
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.calculateIsoDeviceAuthenticationBytes
import at.asitplus.wallet.lib.agent.calculateIsoDeviceSignature
import at.asitplus.wallet.lib.cbor.SignCoseDetached
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.CredentialTrustAnchorSet
import at.asitplus.wallet.lib.validation.CredentialTrustAnchors
import at.asitplus.wallet.lib.validation.CredentialValidator
import at.asitplus.wallet.lib.validation.PresentationChecks
import at.asitplus.wallet.lib.validation.PresentationContext.Companion.forMdoc
import at.asitplus.wallet.lib.validation.ValidationDecision
import io.github.z4kn4fein.semver.Version
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate

class ProximityResponseValidatorTest {

    @Test
    fun sessionTranscriptOfQrEngagementDecodesUnchanged() = assertDecodesUnchanged(Simple.NULL)

    @Test
    fun sessionTranscriptOfNfcEngagementDecodesUnchanged() = assertDecodesUnchanged(
        buildCborArray {
            add(Bstr(Random.nextBytes(32)))
            add(Bstr(Random.nextBytes(32)))
        }
    )

    @Test
    fun sessionTranscriptOfNfcStaticHandoverDecodesUnchanged() = assertDecodesUnchanged(
        buildCborArray {
            add(Bstr(Random.nextBytes(32)))
            add(Simple.NULL)
        }
    )

    /**
     * The holder and the reader derive their session keys from the encoded transcript, and decode it to sign and verify
     * the device authentication: only if it re-encodes to the same bytes do both sign and verify the transcript of
     * their session.
     */
    private fun assertDecodesUnchanged(handover: DataItem) = runTest {
        val encoded = encodeSessionTranscript(
            Random.nextBytes(32),
            handover,
            Crypto.createEcPrivateKey(EcCurve.P256).publicKey
        )

        val decoded = coseCompliantSerializer.decodeFromByteArray<SessionTranscript>(encoded)

        assertContentEquals(encoded, coseCompliantSerializer.encodeToByteArray(decoded))
    }

    @Test
    fun readerKeyAgreesOnTheSecretOfTheMdocForP256() = assertAgreesOnSecret(EcCurve.P256)

    @Test
    fun readerKeyAgreesOnTheSecretOfTheMdocForP384() = assertAgreesOnSecret(EcCurve.P384)

    @Test
    fun readerKeyAgreesOnTheSecretOfTheMdocForP521() = assertAgreesOnSecret(EcCurve.P521)

    /** VC-K verifies a device MAC with the converted reader key, the mdoc computes it with the key of multipaz. */
    private fun assertAgreesOnSecret(curve: EcCurve) = runTest {
        val readerKey = Crypto.createEcPrivateKey(curve)
        val deviceKey = Crypto.createEcPrivateKey(curve)
        val devicePublicKey = deviceKey.publicKey as EcPublicKeyDoubleCoordinate

        val secret = assertNotNull(readerKey.toKeyAgreementPrivateValue()).keyAgreement(
            CryptoPublicKey.EC.fromUncompressed(curve.toSignum(), devicePublicKey.x, devicePublicKey.y)
        ).getOrThrow()

        assertContentEquals(Crypto.keyAgreement(deviceKey, readerKey.publicKey), secret)
    }

    @Test
    fun readerKeyOfUnsupportedCurveCanNotVerifyMac() {
        val brainpoolKey = EcPrivateKeyDoubleCoordinate(EcCurve.BRAINPOOLP256R1, Random.nextBytes(32), ByteArray(32), ByteArray(32))

        assertNull(brainpoolKey.toKeyAgreementPrivateValue())
    }

    @Test
    fun verifiesDocumentSignedOverReaderSessionTranscript() = runTest {
        val transcript = readerSessionTranscript()

        val report = validator().validate(response(document(transcript)), forMdoc(transcript)).single().report

        assertEquals(CheckOutcome.Passed, (report.checks as PresentationChecks).signature, report.toString())
        // Only the status of the test credential can not be resolved
        assertFalse(report.toUi().integrityFailed, report.toString())
    }

    @Test
    fun integrityFailsForDocumentSignedOverAnotherSessionTranscript() = runTest {
        val document = document(readerSessionTranscript())

        val validation = validator().validate(response(document), forMdoc(readerSessionTranscript())).single()

        assertEquals(ValidationDecision.REJECTED, validation.report.decision)
        assertTrue(validation.report.toUi().integrityFailed, "claims must not be shown for a forged device signature")
    }

    @Test
    fun integrityFailsForDocumentSignedByAnotherDeviceKey() = runTest {
        val transcript = readerSessionTranscript()

        val validation = validator()
            .validate(response(document(transcript, signer = EphemeralKeyWithoutCert())), forMdoc(transcript)).single()

        assertTrue(validation.report.toUi().integrityFailed)
    }

    @Test
    fun untrustedIssuerIsOnlyAWarning() = runTest {
        val transcript = readerSessionTranscript()

        val outcome = CredentialValidator(trustAnchors = { _, _ -> null })
            .let(::ProximityResponseValidator)
            .validate(response(document(transcript)), forMdoc(transcript)).single().report.toUi()

        assertFalse(outcome.accepted)
        assertFalse(outcome.integrityFailed, "the claims of an untrusted issuer are shown with a warning")
    }

    @Test
    fun failsForErrorStatusOfResponse() = runTest {
        assertFailsWith<VerifyResponseException> {
            validator().validate(DeviceResponse(parsedVersion = Version(1, 0), status = 10U), forMdoc(readerSessionTranscript()))
        }
    }

    @Test
    fun failsForResponseWithoutDocuments() = runTest {
        assertFailsWith<VerifyResponseException> {
            validator().validate(DeviceResponse(parsedVersion = Version(1, 0), status = 0U), forMdoc(readerSessionTranscript()))
        }
    }

    private val issuerKey = EphemeralKeyWithSelfSignedCert()
    private val holderKey = EphemeralKeyWithoutCert()

    private fun validator() = ProximityResponseValidator(
        CredentialValidator(trustAnchors = CredentialTrustAnchors { _, _ ->
            CredentialTrustAnchorSet(source = "test", certificates = setOfNotNull(issuerKey.getCertificate()))
        })
    )

    private suspend fun readerSessionTranscript() = coseCompliantSerializer.decodeFromByteArray<SessionTranscript>(
        encodeSessionTranscript(
            encodedDeviceEngagement = Random.nextBytes(32),
            handover = Simple.NULL,
            eReaderKey = Crypto.createEcPrivateKey(EcCurve.P256).publicKey,
        )
    )

    private fun EcCurve.toSignum() = when (this) {
        EcCurve.P256 -> ECCurve.SECP_256_R_1
        EcCurve.P384 -> ECCurve.SECP_384_R_1
        EcCurve.P521 -> ECCurve.SECP_521_R_1
        else -> error("Unsupported curve $this")
    }

    private fun response(document: Document) =
        DeviceResponse(parsedVersion = Version(1, 0), documents = arrayOf(document), status = 0U)

    /** A document of a freshly issued mdoc, with the device signature of [signer] over [transcript], as a holder does. */
    private suspend fun document(transcript: SessionTranscript, signer: KeyMaterial = holderKey): Document {
        val issued = IssuerAgent(
            keyMaterial = issuerKey,
            identifier = "https://issuer.example.com/".toUri(),
        ).issueCredential(
            CredentialToBeIssued.Iso(
                issuerSignedItems = listOf(IssuerSignedItem(0U, Random.nextBytes(16), "family_name", "Musterfrau")),
                expiration = Clock.System.now() + 10.minutes,
                scheme = IsoMdocFallbackCredentialScheme(DOCTYPE),
                subjectPublicKey = holderKey.publicKey,
                userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
            )
        ).getOrThrow() as Issuer.IssuedCredential.Iso
        val namespaces = ByteStringWrapper(DeviceNameSpaces(emptyMap()))
        val deviceSignature = calculateIsoDeviceSignature(
            calculateIsoDeviceAuthenticationBytes(IsoDeviceSignatureInput(DOCTYPE, namespaces), transcript).getOrThrow(),
            SignCoseDetached(signer),
        ).getOrThrow()
        return Document(DOCTYPE, issued.issuerSigned, DeviceSigned(namespaces, DeviceAuth(deviceSignature = deviceSignature)))
    }

    private companion object {
        const val DOCTYPE = "org.example.unknown.1"
    }
}
