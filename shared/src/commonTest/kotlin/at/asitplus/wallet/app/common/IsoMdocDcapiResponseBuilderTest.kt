package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover.Companion.TYPE_DCAPI
import at.asitplus.dcapi.DCAPIInfo
import at.asitplus.dcapi.DCAPIResponse
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.iso.IssuerSignedItem
import at.asitplus.iso.serializeOrigin
import at.asitplus.iso.sha256
import at.asitplus.signum.indispensable.CryptoPrivateKey
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.SecretExposure
import at.asitplus.signum.indispensable.cosef.CoseKeyParams
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.lib.RequestOptionsCredential
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.iso.Iso180137AnnexCRequestOptions
import at.asitplus.wallet.lib.iso.Iso180137AnnexCVerifier
import at.asitplus.wallet.lib.openid.CredentialPresentationRequestBuilder
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToByteArray
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.crypto.Hpke
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(SecretExposure::class)
class IsoMdocDcapiResponseBuilderTest {

    @Test
    fun sessionTranscriptMatchesVerifierInputs() = runTest {
        val fixture = dcapiFixture()

        val walletTranscript = IsoMdocDcapiResponseBuilder.sessionTranscriptFor(fixture.walletRequest)
        val verifierTranscript = at.asitplus.iso.SessionTranscript.forDcApi(
            at.asitplus.dcapi.DCAPIHandover(
                type = TYPE_DCAPI,
                hash = coseCompliantSerializer.encodeToByteArray(
                    DCAPIInfo(
                        encryptionInfo = fixture.walletRequest.isoMdocRequest.encryptionInfo,
                        serializedOrigin = ORIGIN.serializeOrigin() ?: error("Invalid origin")
                    )
                ).sha256()
            )
        )

        assertContentEquals(
            coseCompliantSerializer.encodeToByteArray(verifierTranscript),
            coseCompliantSerializer.encodeToByteArray(walletTranscript),
        )
    }

    @Test
    fun hpkeRoundtripRequiresSameSessionTranscriptBytes() = runTest {
        val fixture = dcapiFixture()
        val encodedTranscript = coseCompliantSerializer.encodeToByteArray(
            IsoMdocDcapiResponseBuilder.sessionTranscriptFor(fixture.walletRequest)
        )
        val plaintext = "device-response".encodeToByteArray()

        val encrypter = Hpke.getEncrypter(
            cipherSuite = Hpke.CipherSuite.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM,
            receiverPublicKey = fixture.walletRequest.isoMdocRequest.encryptionInfo.encryptionParameters.recipientMultipazPublicKey(),
            info = encodedTranscript
        )
        val ciphertext = encrypter.encrypt(plaintext, aad = ByteArray(0))

        assertContentEquals(
            plaintext,
            decryptHpke(
                enc = encrypter.encapsulatedKey.toByteArray(),
                ciphertext = ciphertext,
                responseEncryptionKeySignum = fixture.verifierKey.exportPrivateKey().getOrThrow()
                    as CryptoPrivateKey.EC.WithPublicKey,
                cborEncodedSessionTranscript = encodedTranscript,
            )
        )

        assertFailsWith<Throwable> {
            decryptHpke(
                enc = encrypter.encapsulatedKey.toByteArray(),
                ciphertext = ciphertext,
                responseEncryptionKeySignum = fixture.verifierKey.exportPrivateKey().getOrThrow()
                    as CryptoPrivateKey.EC.WithPublicKey,
                cborEncodedSessionTranscript = encodedTranscript + byteArrayOf(0x00),
            )
        }
    }

    @Test
    fun encryptedAnnexCResponseValidatesDeviceSignature() = runTest {
        val fixture = dcapiFixture()
        val holderKey = EphemeralKeyWithoutCert()
        val holderAgent = HolderAgent(holderKey)
        holderAgent.storeCredential(
            IssuerAgent(
                keyMaterial = EphemeralKeyWithoutCert(),
                identifier = "https://issuer.example.com/".toUri(),
            ).issueCredential(isoCredential(holderKey.publicKey))
                .getOrThrow()
                .toStoreCredentialInput()
        ).getOrThrow()

        val encryptedResponse = IsoMdocDcapiResponseBuilder.buildEncryptedResponse(
            credentialPresentation = fixture.presentationRequestBuilder.toPresentationExchangeRequest()
                .toCredentialPresentation() as CredentialPresentation.PresentationExchangePresentation,
            isoMdocWalletRequest = fixture.walletRequest,
            keyMaterial = WalletKeyMaterial(holderKey),
            holderAgent = holderAgent,
        )

        val verified = fixture.verifier.validateResponse(
            receivedData = DCAPIResponse(encryptedResponse),
            externalId = STATE,
            decryptHpke = ::decryptHpke,
            expectedOrigin = ORIGIN,
        ).getOrThrow()

        assertTrue(verified.documents.isNotEmpty())
    }

    private suspend fun dcapiFixture(): DcapiFixture {
        val verifierKey = EphemeralKeyWithoutCert()
        val verifier = Iso180137AnnexCVerifier(decryptionKeyMaterial = verifierKey)
        val presentationRequestBuilder = CredentialPresentationRequestBuilder(
            listOf(
                RequestOptionsCredential(
                    credentialScheme = ConstantIndex.AtomicAttribute2023,
                    representation = ConstantIndex.CredentialRepresentation.ISO_MDOC,
                    requestedAttributes = setOf(ConstantIndex.AtomicAttribute2023.CLAIM_GIVEN_NAME),
                )
            )
        )
        val isoRequest = verifier.createRequest(
            Iso180137AnnexCRequestOptions(
                deviceRequest = presentationRequestBuilder.toIso180137AnnexCDeviceRequest(),
                state = STATE,
            )
        )
        return DcapiFixture(
            verifier = verifier,
            verifierKey = verifierKey,
            presentationRequestBuilder = presentationRequestBuilder,
            walletRequest = DCAPIWalletRequest.IsoMdoc(
                isoMdocRequest = isoRequest,
                callingOrigin = ORIGIN,
                credentialIds = null,
            )
        )
    }

    private fun isoCredential(subjectPublicKey: at.asitplus.signum.indispensable.CryptoPublicKey) =
        CredentialToBeIssued.Iso(
            issuerSignedItems = listOf(
                IssuerSignedItem(
                    digestId = 0U,
                    random = Random.nextBytes(16),
                    elementIdentifier = ConstantIndex.AtomicAttribute2023.CLAIM_GIVEN_NAME,
                    elementValue = "Susanne",
                )
            ),
            expiration = Clock.System.now() + 10.minutes,
            scheme = ConstantIndex.AtomicAttribute2023,
            subjectPublicKey = subjectPublicKey,
            userInfo = at.asitplus.openid.OidcUserInfoExtended.fromOidcUserInfo(
                at.asitplus.openid.OidcUserInfo("subject")
            ).getOrThrow(),
        )

    private data class DcapiFixture(
        val verifier: Iso180137AnnexCVerifier,
        val verifierKey: EphemeralKeyWithoutCert,
        val presentationRequestBuilder: CredentialPresentationRequestBuilder,
        val walletRequest: DCAPIWalletRequest.IsoMdoc,
    )

    companion object {
        private const val ORIGIN = "https://verifier.example.com"
        private const val STATE = "state"
    }
}

private fun at.asitplus.iso.EncryptionParameters.recipientMultipazPublicKey(): EcPublicKey {
    val keyParams = recipientPublicKey.keyParams as CoseKeyParams.EcKeyParams<*>
    return EcPublicKeyDoubleCoordinate(EcCurve.P256, keyParams.x!!, keyParams.y as ByteArray)
}

@OptIn(SecretExposure::class)
private suspend fun decryptHpke(
    enc: ByteArray,
    ciphertext: ByteArray,
    responseEncryptionKeySignum: CryptoPrivateKey.EC.WithPublicKey,
    cborEncodedSessionTranscript: ByteArray,
): ByteArray {
    val ecCurve = when (responseEncryptionKeySignum.curve) {
        ECCurve.SECP_256_R_1 -> EcCurve.P256
        ECCurve.SECP_384_R_1 -> EcCurve.P384
        ECCurve.SECP_521_R_1 -> EcCurve.P521
    }

    val ecPublicKey = EcPublicKey.fromPem(responseEncryptionKeySignum.publicKey.encodeToPEM().getOrThrow(), ecCurve)
    val ecPrivateKey = EcPrivateKey.fromPem(
        responseEncryptionKeySignum.encodeToPEM().getOrThrow(),
        ecPublicKey
    )

    val responseEncryptionKey = AsymmetricKey.anonymous(
        privateKey = ecPrivateKey,
        algorithm = ecCurve.defaultKeyAgreementAlgorithm
    )

    val decrypter = Hpke.getDecrypter(
        cipherSuite = Hpke.CipherSuite.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM,
        receiverPrivateKey = responseEncryptionKey,
        encapsulatedKey = enc,
        info = cborEncodedSessionTranscript,
    )
    return decrypter.decrypt(
        ciphertext = ciphertext,
        aad = ByteArray(0),
    )
}
