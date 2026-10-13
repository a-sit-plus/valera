package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.openid.VerifierSignature
import kotlinx.coroutines.test.runTest
import ui.composables.TrustState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleSignedRelyingPartyTrustTest {

    @Test
    fun signerOfASignedRequestIsListedWithItsCertificate() = runTest {
        val certificate = EphemeralKeyWithSelfSignedCert().getCertificate()!!

        val result = evaluateSingleSignedRelyingParty(
            clientId = "x509_hash:verifier",
            certificateChain = listOf(certificate),
            state = TrustState.UNTRUSTED,
        )

        assertEquals(RelyingPartyTrustSummary.UNTRUSTED, result.summary)
        assertFalse(result.multiSigned)
        val signer = result.signers.single()
        assertEquals("x509_hash:verifier", signer.clientId)
        assertEquals(certificate, signer.certificate)
        assertEquals(VerifierSignature.Status.AUTHENTICATED, signer.signatureStatus)
        assertEquals(TrustState.UNTRUSTED, signer.trustState)
    }

    @Test
    fun requestWithoutCertificateHasNoSigner() {
        val result = evaluateSingleSignedRelyingParty(
            clientId = null,
            certificateChain = null,
            state = TrustState.UNKNOWN,
        )

        assertEquals(RelyingPartyTrustSummary.UNKNOWN, result.summary)
        assertTrue(result.signers.isEmpty())
        assertFalse(result.multiSigned)
    }
}
