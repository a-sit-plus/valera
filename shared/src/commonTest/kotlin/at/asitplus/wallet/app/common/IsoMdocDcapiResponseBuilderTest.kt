package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover.Companion.TYPE_DCAPI
import at.asitplus.dcapi.DCAPIInfo
import at.asitplus.dcapi.DCAPIResponse
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.DeviceNameSpaces
import at.asitplus.iso.DeviceSignedItem
import at.asitplus.iso.DeviceSignedItemList
import at.asitplus.iso.IssuerSignedItem
import at.asitplus.iso.serializeOrigin
import at.asitplus.iso.sha256
import at.asitplus.iso.wrapInCborTag
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.openid.dcql.DCQLClaimsPathPointer
import at.asitplus.openid.dcql.toIso180137AnnexCDeviceRequest
import at.asitplus.signum.indispensable.CryptoPrivateKey
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.signum.indispensable.SecretExposure
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.signum.supreme.asymmetric.HPKE
import at.asitplus.wallet.lib.RequestOptionsCredential
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
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
                        encryptionInfo = fixture.walletRequest.parameters.isoMdocRequest.encryptionInfo,
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

        val sealed = hpke.SealBase(
            pkR = fixture.walletRequest.parameters.isoMdocRequest.encryptionInfo.encryptionParameters
                .recipientPublicKey.toCryptoPublicKey().getOrThrow() as CryptoPublicKey.EC,
            info = encodedTranscript,
            aad = ByteArray(0),
            pt = plaintext,
        )

        assertContentEquals(
            plaintext,
            decryptHpke(
                enc = sealed.encapsulatedSecret,
                ciphertext = sealed.ciphertext,
                responseEncryptionKeySignum = fixture.verifierKey.exportPrivateKey().getOrThrow()
                    as CryptoPrivateKey.EC.WithPublicKey,
                cborEncodedSessionTranscript = encodedTranscript,
            )
        )

        assertFailsWith<Throwable> {
            decryptHpke(
                enc = sealed.encapsulatedSecret,
                ciphertext = sealed.ciphertext,
                responseEncryptionKeySignum = fixture.verifierKey.exportPrivateKey().getOrThrow()
                    as CryptoPrivateKey.EC.WithPublicKey,
                cborEncodedSessionTranscript = encodedTranscript + byteArrayOf(0x00),
            )
        }
    }

    @Test
    fun deviceAuthenticationPayloadKeepsDeviceNameSpaceBytesStable() = runTest {
        val fixture = dcapiFixture()
        val deviceNameSpaceBytes = ByteStringWrapper(
            DeviceNameSpaces(
                mapOf(
                    ConstantIndex.AtomicAttribute2023.isoNamespace to DeviceSignedItemList(
                        listOf(DeviceSignedItem(ConstantIndex.AtomicAttribute2023.CLAIM_GIVEN_NAME, "Susanne"))
                    )
                )
            )
        )
        val deviceAuthentication = DeviceAuthentication(
            type = DeviceAuthentication.TYPE,
            sessionTranscript = IsoMdocDcapiResponseBuilder.sessionTranscriptFor(fixture.walletRequest),
            docType = ConstantIndex.AtomicAttribute2023.isoDocType,
            namespaces = deviceNameSpaceBytes,
        )

        val walletSidePayload = coseCompliantSerializer
            .encodeToByteArray(ByteStringWrapper(deviceAuthentication))
            .wrapInCborTag(24)
        val verifierSidePayload = coseCompliantSerializer
            .encodeToByteArray(coseCompliantSerializer.encodeToByteArray(deviceAuthentication))
            .wrapInCborTag(24)

        assertContentEquals(verifierSidePayload, walletSidePayload)
    }

    @Test
    fun encryptedAnnexCResponseValidatesDeviceSignature() = runTest {
        val fixture = dcapiFixture()
        val holderKey = EphemeralKeyWithoutCert()
        val holderAgent = HolderAgent(holderKey)
        holderAgent.storeCredential(
            IssuerAgent(
                keyMaterial = EphemeralKeyWithSelfSignedCert(),
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
                    attributePaths = setOf(DCQLClaimsPathPointer(ConstantIndex.AtomicAttribute2023.CLAIM_GIVEN_NAME)),
                )
            )
        )
        val isoRequest = verifier.createRequest(
            Iso180137AnnexCRequestOptions(
                deviceRequest = presentationRequestBuilder.toDCQLRequest()!!.dcqlQuery.toIso180137AnnexCDeviceRequest(),
                state = STATE,
            )
        )
        return DcapiFixture(
            verifier = verifier,
            verifierKey = verifierKey,
            presentationRequestBuilder = presentationRequestBuilder,
            walletRequest = RequestParametersFrom.IsoMdocDcApi(
                parameters = RequestParametersFrom.IsoMdocDcApi.IsoMdocRequestWrapper(isoRequest),
                jsonString = joseCompliantSerializer.encodeToString(isoRequest),
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
        val walletRequest: RequestParametersFrom.IsoMdocDcApi,
    )

    companion object {
        private const val ORIGIN = "https://verifier.example.com"
        private const val STATE = "state"
    }
}

@OptIn(SecretExposure::class)
private suspend fun decryptHpke(
    enc: ByteArray,
    ciphertext: ByteArray,
    responseEncryptionKeySignum: CryptoPrivateKey.EC.WithPublicKey,
    cborEncodedSessionTranscript: ByteArray,
): ByteArray =
    hpke.OpenBase(
        enc = enc,
        skR = responseEncryptionKeySignum,
        info = cborEncodedSessionTranscript,
        aad = ByteArray(0),
        ct = ciphertext,
    )

private val hpke = HPKE(
    HPKE.KEM.DHKEM_P256_HKDF_SHA256,
    HPKE.KDF.HKDF_SHA256,
    HPKE.AEAD.AES_128_GCM,
)
