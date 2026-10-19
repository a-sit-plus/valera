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
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.iso.transfer.encodeReaderSessionTranscript
import at.asitplus.wallet.app.common.iso.transfer.sessionTranscript
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
import at.asitplus.wallet.lib.validation.ValidationDecision
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToByteArray
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ProximityResponseValidatorTest {

    @Test
    fun sessionTranscriptOfQrEngagementIsEncodedAsTheReaderEncodesIt() = assertEncodedAlike(Simple.NULL)

    @Test
    fun sessionTranscriptOfNfcEngagementIsEncodedAsTheReaderEncodesIt() = assertEncodedAlike(
        buildCborArray {
            add(Bstr(Random.nextBytes(32)))
            add(Bstr(Random.nextBytes(32)))
        }
    )

    @Test
    fun sessionTranscriptOfNfcStaticHandoverIsEncodedAsTheReaderEncodesIt() = assertEncodedAlike(
        buildCborArray {
            add(Bstr(Random.nextBytes(32)))
            add(Simple.NULL)
        }
    )

    /**
     * The holder signs the [sessionTranscript] it encodes with VC-K, the reader derives its session keys from the
     * transcript it encodes with multipaz: only if both are alike does the reader verify the device signature over
     * the same transcript the holder signed.
     */
    private fun assertEncodedAlike(handover: DataItem) = runTest {
        val engagement = Random.nextBytes(32)
        val eReaderKey = Crypto.createEcPrivateKey(EcCurve.P256).publicKey

        assertContentEquals(
            encodeReaderSessionTranscript(engagement, handover, eReaderKey),
            coseCompliantSerializer.encodeToByteArray(sessionTranscript(engagement, handover, eReaderKey)),
        )
    }

    @Test
    fun verifiesDocumentSignedOverReaderSessionTranscript() = runTest {
        val transcript = readerSessionTranscript()

        val report = validator().validate(response(document(transcript)), transcript).single().report

        assertEquals(CheckOutcome.Passed, (report.checks as PresentationChecks).signature, report.toString())
        // Only the status of the test credential can not be resolved
        assertFalse(report.toUi().integrityFailed, report.toString())
    }

    @Test
    fun integrityFailsForDocumentSignedOverAnotherSessionTranscript() = runTest {
        val document = document(readerSessionTranscript())

        val validation = validator().validate(response(document), readerSessionTranscript()).single()

        assertEquals(ValidationDecision.REJECTED, validation.report.decision)
        assertTrue(validation.report.toUi().integrityFailed, "claims must not be shown for a forged device signature")
    }

    @Test
    fun integrityFailsForDocumentSignedByAnotherDeviceKey() = runTest {
        val transcript = readerSessionTranscript()

        val validation = validator()
            .validate(response(document(transcript, signer = EphemeralKeyWithoutCert())), transcript).single()

        assertTrue(validation.report.toUi().integrityFailed)
    }

    @Test
    fun rejectsDocumentWithoutSessionTranscript() = runTest {
        val validation = validator().validate(response(document(readerSessionTranscript())), null).single()

        assertEquals(ValidationDecision.REJECTED, validation.report.decision)
    }

    @Test
    fun untrustedIssuerIsOnlyAWarning() = runTest {
        val transcript = readerSessionTranscript()

        val outcome = CredentialValidator(trustAnchors = { _, _ -> null })
            .let(::ProximityResponseValidator)
            .validate(response(document(transcript)), transcript).single().report.toUi()

        assertFalse(outcome.accepted)
        assertFalse(outcome.integrityFailed, "the claims of an untrusted issuer are shown with a warning")
    }

    @Test
    fun failsForErrorStatusOfResponse() = runTest {
        assertFailsWith<VerifyResponseException> {
            validator().validate(DeviceResponse(parsedVersion = Version(1, 0), status = 10U), null)
        }
    }

    @Test
    fun failsForResponseWithoutDocuments() = runTest {
        assertFailsWith<VerifyResponseException> {
            validator().validate(DeviceResponse(parsedVersion = Version(1, 0), status = 0U), null)
        }
    }

    private val issuerKey = EphemeralKeyWithSelfSignedCert()
    private val holderKey = EphemeralKeyWithoutCert()

    private fun validator() = ProximityResponseValidator(
        CredentialValidator(trustAnchors = CredentialTrustAnchors { _, _ ->
            CredentialTrustAnchorSet(source = "test", certificates = setOfNotNull(issuerKey.getCertificate()))
        })
    )

    private suspend fun readerSessionTranscript() = sessionTranscript(
        encodedDeviceEngagement = Random.nextBytes(32),
        handover = Simple.NULL,
        eReaderKey = Crypto.createEcPrivateKey(EcCurve.P256).publicKey,
    )

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
