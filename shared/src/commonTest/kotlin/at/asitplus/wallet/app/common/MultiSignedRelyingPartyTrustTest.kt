package at.asitplus.wallet.app.common

import at.asitplus.signum.indispensable.josef.JwsAlgorithm
import at.asitplus.signum.indispensable.josef.JwsHeader
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.openid.VerifierSignature
import kotlinx.coroutines.test.runTest
import ui.composables.TrustState
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiSignedRelyingPartyTrustTest {

    private suspend fun header(clientId: String): Pair<JwsHeader.Part, X509Certificate> {
        val certificate = EphemeralKeyWithSelfSignedCert().getCertificate()!!
        return JwsHeader.Part(
            algorithm = JwsAlgorithm.Signature.ES256,
            certificateChain = listOf(certificate),
            clientId = clientId,
        ) to certificate
    }

    private fun signature(index: Int, clientId: String, status: VerifierSignature.Status) = VerifierSignature(
        signatureIndex = index,
        clientId = clientId,
        verifierInfo = null,
        status = status,
    )

    @Test
    fun invalidSignerIsKeptOutOfTheTrustDecision() = runTest {
        val (own, ownCertificate) = header("x509_hash:own")
        // a trusted verifier's header, copied next to a signature that does not verify
        val (copied, copiedCertificate) = header("x509_hash:trusted")
        val evaluated = mutableListOf<X509Certificate>()

        val result = evaluateMultiSignedRelyingParty(
            headers = listOf(own, copied),
            verifierSignatures = listOf(
                signature(0, "x509_hash:own", VerifierSignature.Status.AUTHENTICATED),
                signature(1, "x509_hash:trusted", VerifierSignature.Status.INVALID),
            ),
        ) { certificate ->
            evaluated += certificate
            if (certificate == copiedCertificate) TrustState.TRUSTED else TrustState.UNTRUSTED
        }

        assertEquals(listOf(ownCertificate), evaluated)
        assertEquals(
            listOf(VerifierSignature.Status.AUTHENTICATED, VerifierSignature.Status.INVALID),
            result.signers.map { it.signatureStatus },
        )
        // the forged, supposedly trusted, identity neither shows as trusted nor turns the summary mixed
        assertEquals(listOf(TrustState.UNTRUSTED, null), result.signers.map { it.trustState })
        assertEquals(RelyingPartyTrustSummary.UNTRUSTED, result.summary)
    }

    @Test
    fun authenticatedSignersAreSummarizedTogether() = runTest {
        val (untrusted, _) = header("x509_hash:untrusted")
        val (trusted, trustedCertificate) = header("x509_hash:trusted")

        val result = evaluateMultiSignedRelyingParty(
            headers = listOf(untrusted, trusted),
            verifierSignatures = listOf(
                signature(0, "x509_hash:untrusted", VerifierSignature.Status.AUTHENTICATED),
                signature(1, "x509_hash:trusted", VerifierSignature.Status.AUTHENTICATED),
            ),
        ) { if (it == trustedCertificate) TrustState.TRUSTED else TrustState.UNTRUSTED }

        assertEquals(RelyingPartyTrustSummary.MIXED_WITH_TRUSTED, result.summary)
    }

    @Test
    fun signatureWithoutReportedOutcomeIsInvalid() = runTest {
        val (first, _) = header("x509_hash:first")

        val result = evaluateMultiSignedRelyingParty(
            headers = listOf(first),
            verifierSignatures = emptyList(),
        ) { TrustState.TRUSTED }

        assertEquals(VerifierSignature.Status.INVALID, result.signers.single().signatureStatus)
        assertEquals(RelyingPartyTrustSummary.UNKNOWN, result.summary)
    }

    @Test
    fun unsupportedSignerIsNeitherEvaluatedNorCountedAsInvalid() = runTest {
        val (other, _) = header("openid_federation:https://other.example")
        val (own, _) = header("x509_hash:own")

        val result = evaluateMultiSignedRelyingParty(
            headers = listOf(other, own),
            verifierSignatures = listOf(
                signature(0, "openid_federation:https://other.example", VerifierSignature.Status.UNSUPPORTED),
                signature(1, "x509_hash:own", VerifierSignature.Status.AUTHENTICATED),
            ),
        ) { TrustState.TRUSTED }

        assertEquals(listOf(null, TrustState.TRUSTED), result.signers.map { it.trustState })
        assertEquals(RelyingPartyTrustSummary.TRUSTED, result.summary)
    }
}
