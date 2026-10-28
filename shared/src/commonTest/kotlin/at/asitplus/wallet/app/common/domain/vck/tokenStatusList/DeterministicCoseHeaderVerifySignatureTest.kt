package at.asitplus.wallet.app.common.domain.vck.tokenStatusList

import at.asitplus.iso.stripCborTag
import at.asitplus.signum.indispensable.cosef.CoseSigned
import at.asitplus.wallet.lib.data.StatusListCwt
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.StatusListInfo
import at.asitplus.wallet.lib.data.rfc.tokenStatusList.TokenStatusInfo
import at.asitplus.wallet.lib.validation.CheckOutcome
import at.asitplus.wallet.lib.validation.SignatureCheck
import at.asitplus.wallet.lib.validation.StatusListTokenChecks
import at.asitplus.wallet.lib.validation.StatusPolicy
import at.asitplus.wallet.lib.validation.StatusSignerAnchors
import at.asitplus.wallet.lib.validation.StatusValidator
import at.asitplus.wallet.lib.validation.TrustPolicy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DeterministicCoseHeaderVerifySignatureTest {
    /**
     * A status list CWT of the EUDI Wallet reference implementation (dev.revocation.eudiw.dev), whose protected header
     * has the entries 1 (alg), 16 (type), 33 (x5chain), which Signum re-encodes as 1, 33, 16.
     */
    private val eudiwCwt = Base64.decode(
            "0oRZA5WjASYQeBphcHBsaWNhdGlvbi9zdGF0dXNsaXN0K2N3dBghWQNwMIIDbDCCAxKgAwIBAgIUe4Kzlw4Ssz8pooCGrSEpkjac" +
            "V/EwCgYIKoZIzj0EAwIwVzEZMBcGA1UEAwwQUElEIElzc3VlciBDQSAwMjEtMCsGA1UECgwkRVVESSBXYWxsZXQgUmVmZXJlbmNl" +
            "IEltcGxlbWVudGF0aW9uMQswCQYDVQQGEwJMVTAeFw0yNjA5MjkxMjAxMjFaFw0yNzEyMjMxMjAxMjBaMHExGTAXBgNVBAMMEFBJ" +
            "RCBEUyAtIDAwMyBERVYxGDAWBgNVBGEMD0xFSUVVLTEyMzQ1Njc4OTEtMCsGA1UECgwkRVVESSBXYWxsZXQgUmVmZXJlbmNlIElt" +
            "cGxlbWVudGF0aW9uMQswCQYDVQQGEwJMVTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABORnbm/HI77Uib1+xkP6lJ68zsrLv0G8" +
            "DcTYAomX/wF48wOCIps9L6I2DuxE+8L5TQiloXIuOGP1A5wAqfbq33GjggGgMIIBnDAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaA" +
            "FFxi+9GmJnpnPzVXFDj4B1jgh7PwMFkGCCsGAQUFBwEBBE0wSzBJBggrBgEFBQcwAoY9aHR0cHM6Ly9wcmVwcm9kLnBraS5ldWRp" +
            "dy5kZXYvYWlhL1BJRElzc3VlckNBMDItTFUuY2FjZXJ0LnBlbTAqBgNVHREEIzAhhh9odHRwczovL2VjLmRldi5pc3N1ZXIuZXVk" +
            "aXcuZGV2MC4GA1UdIAQnMCUwIwYDKgMEMBwwGgYIKwYBBQUHAgEWDmV4YW1wbGUucG9saWN5MBsGA1UdJQQUMBIGByiBjF0FAQIG" +
            "ByiBtTQEAQIwQwYDVR0fBDwwOjA4oDagNIYyaHR0cHM6Ly9wcmVwcm9kLnBraS5ldWRpdy5kZXYvY3JsL3BpZF9DQV9MVV8wMi5j" +
            "cmwwHQYDVR0OBBYEFLoPAUp/N1j54IHVy0T9SpX+8Wk5MA4GA1UdDwEB/wQEAwIHgDAjBggrBgEFBQcBAwQXMBUwEwYGBACORgEG" +
            "MAkGBwQAi+xOAQEwCgYIKoZIzj0EAwIDSAAwRQIhAIg5MqvG9q06gf782ys0IKPbPQNawEkIzV+0Z8JmwIl/AiAGCdqzEgH2pFxx" +
            "sYJ+vEilJ3QV72xOsSKnPdfWrMsqAaBZAbClAnhwaHR0cHM6Ly9kZXYucmV2b2NhdGlvbi5ldWRpdy5kZXYvdG9rZW5fc3RhdHVz" +
            "X2xpc3QvRkMvb3JnLmlzby4xODAxMy41LjEubURMLzVmYTM4OWY0LTVkODAtNGI0Ni04YzI2LTU5YjY3ZTE5NTM3ZAQaauNGygYa" +
            "auH1Shn//hkOEBn//aJkYml0cwFjbHN0WQEZeNqNU0GOBCEIBGI2HDnu0Uz2AfMED3OYZ3Hch+3DRrFVdOyepRPFFigoEAhAgSFL" +
            "LEs+5rNq3tD+2kUAeFY92HorSzJ7AifyaB7NlNmCBu+9CpdYUca5oMNfPaRqsBHtGh2Ip5KAVsgAk1OHsBSlaug8RurYMzX/akSy" +
            "BZZlX3Aq3xPOXB8uPLk8OH/oKNDek/jO78yZbvHw3Txa6kfsUURnEw2RItxDaWG7xZGYte/I+duxNgldd+9sbgZC6izPpZXp415T" +
            "QwkTqz++SdzK/C26xoksWYg6GUwGuSqEPtQ6GfB24vHiLewZbvM8hqSwTiOcbGgOm2hhoKfLKXKpq6+MycVi/g8P7ZlZHPk6fWRR" +
            "ZcPHC3OYFRhYQH7yBmD6FZxd4AQulNQiOemIg3Ov7Onnj7I/ta3KBRNHGRI3zz12gM9mxeH5ZLh1BpLthMAIe2skglSeZo+TPCc="
    ).stripCborTag(18)

    private fun cose(bytes: ByteArray = eudiwCwt) = CoseSigned.deserialize(ByteArraySerializer(), bytes).getOrThrow()

    private val signatureCheck = SignatureCheck(DeterministicCoseHeaderVerifySignature())

    @Test
    fun verifiesAProtectedHeaderInDeterministicOrder() = runTest {
        assertEquals(CheckOutcome.Passed, signatureCheck.verify(cose()).outcome)
    }

    @Test
    fun rejectsAnInvalidSignature() = runTest {
        val tampered = eudiwCwt.copyOf().also { it[it.lastIndex] = (it[it.lastIndex] + 1).toByte() }

        assertIs<CheckOutcome.Failed>(signatureCheck.verify(cose(tampered)).outcome)
    }

    @Test
    fun reordersOnlyTheProtectedHeaderOfASignature1Structure() {
        val reencoded = cose().prepareCoseSignatureInput(byteArrayOf())
        val reordered = assertNotNull(reencoded.withDeterministicProtectedHeader())

        assertEquals(reencoded.size, reordered.size)
        assertNull(reordered.withDeterministicProtectedHeader())
        assertNull("eyJhbGciOiJFUzI1NiJ9.e30".encodeToByteArray().withDeterministicProtectedHeader())
        assertNull(byteArrayOf(0x84.toByte(), 0x6a).withDeterministicProtectedHeader())
    }

    @Test
    fun acceptsTheSignatureOfAStatusListCwt() = runTest {
        val token = StatusListCwt(cose(), Clock.System.now())
        val payload = token.parsedPayload.getOrThrow()
        val leaf = signatureCheck.verify(token.value).certificateChain!!.first()

        val validation = StatusValidator(
            statusListTokenResolver = { token },
            signatureCheck = signatureCheck,
        ).validate(
            TokenStatusInfo(statusList = StatusListInfo(0u, payload.subject)),
            StatusSignerAnchors.OfArtifactKind({ setOf(leaf) }, "test"),
            StatusPolicy.RequireClaim(TrustPolicy.RequireAuthorizedSigner),
            5.minutes,
            Clock.System.now(),
        )

        val checks = assertIs<StatusListTokenChecks>(validation.mechanisms.single().token.checks)
        assertEquals(CheckOutcome.Passed, checks.parsing)
        assertEquals(CheckOutcome.Passed, checks.signature)
    }
}
