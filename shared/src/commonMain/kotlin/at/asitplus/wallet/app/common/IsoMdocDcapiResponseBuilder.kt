package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.dcapi.DCAPIHandover.Companion.TYPE_DCAPI
import at.asitplus.dcapi.DCAPIInfo
import at.asitplus.dcapi.EncryptedResponse
import at.asitplus.dcapi.EncryptedResponseData
import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.serializeOrigin
import at.asitplus.iso.sha256
import at.asitplus.iso.wrapInCborTag
import at.asitplus.signum.indispensable.cosef.CoseKeyParams
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.io.Base64UrlStrict
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.PresentationException
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.cbor.CoseHeaderNone
import at.asitplus.wallet.lib.cbor.SignCoseDetached
import at.asitplus.wallet.lib.data.CredentialPresentation
import io.github.aakira.napier.Napier
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.crypto.Hpke

internal object IsoMdocDcapiResponseBuilder {

    fun sessionTranscriptFor(isoMdocWalletRequest: DCAPIWalletRequest.IsoMdoc): SessionTranscript {
        val callingOrigin = isoMdocWalletRequest.callingOrigin.serializeOrigin()
            ?: throw IllegalArgumentException("Invalid calling origin")
        val hash = coseCompliantSerializer.encodeToByteArray(
            DCAPIInfo(isoMdocWalletRequest.isoMdocRequest.encryptionInfo, callingOrigin)
        ).sha256()
        val handover = DCAPIHandover(type = TYPE_DCAPI, hash = hash)
        return SessionTranscript.forDcApi(handover)
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun buildEncryptedResponse(
        credentialPresentation: CredentialPresentation.PresentationExchangePresentation,
        isoMdocWalletRequest: DCAPIWalletRequest.IsoMdoc,
        keyMaterial: WalletKeyMaterial,
        holderAgent: HolderAgent,
    ): EncryptedResponse {
        val sessionTranscript = sessionTranscriptFor(isoMdocWalletRequest)
        val callingOrigin = isoMdocWalletRequest.callingOrigin.serializeOrigin()
            ?: throw IllegalArgumentException("Invalid calling origin")

        val presentationResult = holderAgent.createPresentation(
            request = PresentationRequestParameters(
                nonce = isoMdocWalletRequest.isoMdocRequest.encryptionInfo.encryptionParameters.nonce
                    ?.encodeToString(Base64UrlStrict) ?: throw IllegalArgumentException("no nonce"),
                audience = callingOrigin,
                calcIsoDeviceSignaturePlain = { input ->
                    val deviceAuthentication = DeviceAuthentication(
                        type = DeviceAuthentication.TYPE,
                        sessionTranscript = sessionTranscript,
                        docType = input.docType,
                        namespaces = input.deviceNameSpaceBytes
                    )

                    val deviceAuthenticationBytes = coseCompliantSerializer
                        .encodeToByteArray(ByteStringWrapper(deviceAuthentication))
                        .wrapInCborTag(24)
                    Napier.d("Device authentication signature input is ${deviceAuthenticationBytes.toHexString()}")
                    SignCoseDetached<ByteArray>(keyMaterial, CoseHeaderNone(), CoseHeaderNone())
                        .invoke(null, null, deviceAuthenticationBytes, ByteArraySerializer())
                        .getOrElse { e ->
                            Napier.w("Could not create DeviceAuth for presentation", e)
                            throw PresentationException(e)
                        }
                },
            ),
            credentialPresentation = credentialPresentation,
        )

        val presentation =
            presentationResult.getOrThrow() as PresentationResponseParameters.PresentationExchangeParameters

        val deviceResponse = when (val firstResult = presentation.presentationResults.firstOrNull()
            ?: throw PresentationException(IllegalStateException("Presentation did not return any device response"))) {
            is CreatePresentationResult.DeviceResponse -> firstResult.deviceResponse
            else -> throw PresentationException(IllegalStateException("Must be a device response"))
        }
        val deviceResponseSerialized = coseCompliantSerializer.encodeToByteArray(deviceResponse)

        val encryptionParameters = isoMdocWalletRequest.isoMdocRequest.encryptionInfo.encryptionParameters

        val publicKey = try {
            val keyParams = encryptionParameters.recipientPublicKey.keyParams as CoseKeyParams.EcKeyParams<*>
            EcPublicKeyDoubleCoordinate(EcCurve.P256, keyParams.x!!, keyParams.y as ByteArray)
        } catch (e: Throwable) {
            Napier.e("Could not extract public key", e)
            throw IllegalArgumentException("Could not extract public key")
        }
        val encodedSessionTranscript = coseCompliantSerializer.encodeToByteArray(sessionTranscript)
        val encrypter = Hpke.getEncrypter(
            cipherSuite = Hpke.CipherSuite.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM,
            receiverPublicKey = publicKey,
            info = encodedSessionTranscript
        )
        val ciphertext = encrypter.encrypt(
            plaintext = deviceResponseSerialized,
            aad = ByteArray(0),
        )
        val encryptedResponseData = EncryptedResponseData(
            enc = encrypter.encapsulatedKey.toByteArray(),
            cipherText = ciphertext
        )
        return EncryptedResponse(TYPE_DCAPI, encryptedResponseData)
    }
}
