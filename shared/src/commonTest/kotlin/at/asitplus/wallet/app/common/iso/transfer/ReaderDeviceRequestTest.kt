package at.asitplus.wallet.app.common.iso.transfer

import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.SessionTranscript
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.app.common.validation.ValidationPolicies
import at.asitplus.wallet.app.common.validation.integrityRejection
import at.asitplus.wallet.lib.validation.AccessCertificateChecks
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.RelyingPartyValidator
import data.document.RequestDocument
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromByteArray
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.util.MdocUtil
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * The reader of this wallet authenticates its requests with a reader certificate that is no WRPAC, which holders only
 * warn about, as long as its signature verifies.
 */
class ReaderDeviceRequestTest {

    @Test
    fun readerAuthenticationOfQrEngagementVerifies() = assertReaderAuthenticationVerifies(Simple.NULL)

    @Test
    fun readerAuthenticationOfNfcEngagementVerifies() = assertReaderAuthenticationVerifies(
        buildCborArray {
            add(Bstr(Random.nextBytes(32)))
            add(Bstr(Random.nextBytes(32)))
        }
    )

    private fun assertReaderAuthenticationVerifies(handover: DataItem) = runTest {
        val encodedSessionTranscript = encodeSessionTranscript(
            Random.nextBytes(32),
            handover,
            Crypto.createEcPrivateKey(EcCurve.P256).publicKey,
        )

        val encodedRequest = encodeReaderDeviceRequest(
            encodedSessionTranscript,
            listOf(RequestDocument(MDL_DOCTYPE, mapOf(MDL_NAMESPACE to mapOf("family_name" to false)))),
            readerKey(),
        )

        val report = RelyingPartyValidator().validate(
            coseCompliantSerializer.decodeFromByteArray<DeviceRequest>(encodedRequest),
            coseCompliantSerializer.decodeFromByteArray<SessionTranscript>(encodedSessionTranscript),
            ValidationPolicies.strict,
        ).report
        val accessCertificate = report.children.single().checks as AccessCertificateChecks
        assertEquals(CheckOutcome.Passed, accessCertificate.signature, report.toString())
        // As the holder decides, see PresentationViewModel.initWithDeviceRequest
        assertNull(report.integrityRejection(), report.toString())
    }

    /** A reader key certified like the one of [TransferManager], by a reader root that is no WRPAC provider. */
    private suspend fun readerKey(): AsymmetricKey.X509CertifiedExplicit {
        // Whole seconds, as certificates have no fractional seconds
        val now = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
        val rootKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val rootCert = MdocUtil.generateIacaCertificate(
            iacaKey = AsymmetricKey.anonymous(rootKey),
            subject = X500Name.fromName("CN=Test Reader Root"),
            serial = ASN1Integer(1L),
            validFrom = now - 1.days,
            validUntil = now + 1.days,
            issuerAltNameUrl = "https://example.com",
            crlUrl = "https://example.com/crl",
        )
        val readerKey = Crypto.createEcPrivateKey(EcCurve.P256)
        val readerCert = MdocUtil.generateReaderCertificate(
            readerRootKey = AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(rootCert)), rootKey),
            readerKey = readerKey.publicKey,
            subject = X500Name.fromName("CN=Test Reader"),
            dnsName = "reader.example.com",
            serial = ASN1Integer(2L),
            validFrom = now - 1.days,
            validUntil = now + 1.days,
        )
        return AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(readerCert, rootCert)), readerKey)
    }

    private companion object {
        const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
        const val MDL_NAMESPACE = "org.iso.18013.5.1"
    }
}
