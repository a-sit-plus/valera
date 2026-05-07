package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.DeviceNameSpaces
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.wrapInCborTag
import at.asitplus.signum.indispensable.Digest
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.cosef.toCoseKey
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.os.PlatformSigningProvider
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.lib.agent.SignerBasedKeyMaterial
import at.asitplus.wallet.lib.cbor.CoseHeaderNone
import at.asitplus.wallet.lib.cbor.SignCoseDetached
import at.asitplus.wallet.lib.cbor.VerifyCoseSignatureWithKey
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertTrue

class IosPlatformDeviceSignatureTest {

    @Test
    fun platformBackedEcdsaSignatureVerifiesAsDetachedCosePayload() = runTest {
        val alias = "valera-ios-dcapi-device-auth-test"
        PlatformSigningProvider.deleteSigningKey(alias)
        val signer = PlatformSigningProvider.createSigningKey(alias = alias) {
            ec {
                curve = ECCurve.SECP_256_R_1
                digests = setOf(Digest.SHA256)
            }
        }.getOrThrow()

        try {
            val keyMaterial = object : SignerBasedKeyMaterial(signer as Signer, alias) {
                override suspend fun getCertificate(): X509Certificate? = null
            }
            val payload = coseCompliantSerializer.encodeToByteArray(
                ByteStringWrapper(
                    DeviceAuthentication(
                        type = DeviceAuthentication.TYPE,
                        sessionTranscript = SessionTranscript.forDcApi(
                            DCAPIHandover(DCAPIHandover.TYPE_DCAPI, ByteArray(32) { 1 })
                        ),
                        docType = "org.iso.18013.5.1.mDL",
                        namespaces = ByteStringWrapper(DeviceNameSpaces(mapOf())),
                    )
                )
            ).wrapInCborTag(24)

            val signature = SignCoseDetached<ByteArray>(keyMaterial, CoseHeaderNone(), CoseHeaderNone())
                .invoke(null, null, payload, ByteArraySerializer())
                .getOrThrow()

            assertTrue(
                VerifyCoseSignatureWithKey<ByteArray>()(
                    coseSigned = signature,
                    signer = keyMaterial.publicKey.toCoseKey().getOrThrow(),
                    externalAad = byteArrayOf(),
                    detachedPayload = payload,
                ).isSuccess
            )
        } finally {
            PlatformSigningProvider.deleteSigningKey(alias)
        }
    }
}
