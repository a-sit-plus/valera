package at.asitplus.wallet.app.common

import at.asitplus.dcapi.DCAPIHandover
import at.asitplus.iso.DeviceAuthentication
import at.asitplus.iso.DeviceNameSpaces
import at.asitplus.iso.SessionTranscript
import at.asitplus.iso.wrapInCborTag
import at.asitplus.signum.indispensable.cosef.io.ByteStringWrapper
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.signum.indispensable.cosef.toCoseKey
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
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
    fun ecdsaSignatureVerifiesAsDetachedCosePayload() = runTest {
        val keyMaterial = EphemeralKeyWithoutCert(customKeyId = "valera-ios-dcapi-device-auth-test")
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
    }
}
