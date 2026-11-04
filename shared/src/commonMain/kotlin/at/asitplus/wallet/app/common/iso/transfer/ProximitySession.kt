package at.asitplus.wallet.app.common.iso.transfer

import at.asitplus.signum.indispensable.CryptoPrivateKey
import at.asitplus.signum.indispensable.ECCurve
import at.asitplus.signum.indispensable.KeyAgreementPrivateValue
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import data.document.RequestDocument
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.mdoc.request.DeviceRequest

/**
 * The encoded `SessionTranscript` of an ISO/IEC 18013-5 session (9.1.5.1) after QR engagement ([handover] is
 * [Simple.NULL]) or NFC engagement, from which the holder and the reader of this wallet derive their session keys.
 * Decoded as [at.asitplus.iso.SessionTranscript], it is the transcript the holder signs its documents over, and the
 * reader verifies them with, see [at.asitplus.wallet.app.common.iso.verifier.ProximityResponseValidator].
 */
internal fun encodeSessionTranscript(
    encodedDeviceEngagement: ByteArray,
    handover: DataItem,
    eReaderKey: EcPublicKey,
): ByteArray {
    val encodedEReaderKey = Cbor.encode(eReaderKey.toCoseKey().toDataItem())
    return Cbor.encode(
        buildCborArray {
            add(Tagged(24, Bstr(encodedDeviceEngagement)))
            add(Tagged(24, Bstr(encodedEReaderKey)))
            add(handover)
        }
    )
}

/**
 * The encoded `DeviceRequest` of a reader for [documents], each authenticated with [readerKey] over the
 * `ReaderAuthentication` (ISO/IEC 18013-5, 9.1.4), which contains the session transcript itself, not its encoding as a
 * byte string, so that holders verify it.
 */
internal suspend fun encodeReaderDeviceRequest(
    encodedSessionTranscript: ByteArray,
    documents: List<RequestDocument>,
    readerKey: AsymmetricKey.X509CertifiedExplicit,
): ByteArray {
    val builder = DeviceRequest.Builder(sessionTranscript = Cbor.decode(encodedSessionTranscript))
    documents.forEach { document ->
        builder.addDocRequest(
            docType = document.docType,
            nameSpaces = document.itemsToRequest,
            docRequestInfo = null,
            readerKey = readerKey
        )
    }
    return Cbor.encode(builder.build().toDataItem())
}

/**
 * The reader's ephemeral key `EReaderKey.Priv` for verifying a device MAC (ISO/IEC 18013-5, 9.1.3.5), or `null` for a
 * curve that VC-K does not support for key agreement, so that documents authenticated with a MAC can not be verified.
 */
internal fun EcPrivateKey.toKeyAgreementPrivateValue(): KeyAgreementPrivateValue.ECDH? {
    val curve = when (curve) {
        EcCurve.P256 -> ECCurve.SECP_256_R_1
        EcCurve.P384 -> ECCurve.SECP_384_R_1
        EcCurve.P521 -> ECCurve.SECP_521_R_1
        else -> return null
    }
    return CryptoPrivateKey.EC.WithPublicKey(
        privateKey = BigInteger.fromByteArray(d, Sign.POSITIVE),
        curve = curve,
        encodeCurve = true,
        encodePublicKey = true,
    )
}
