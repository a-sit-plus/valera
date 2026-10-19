package at.asitplus.wallet.app.common.iso.transfer

import at.asitplus.dcapi.NFCHandover
import at.asitplus.iso.SessionTranscript
import at.asitplus.signum.indispensable.cosef.CoseKey
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.crypto.EcPublicKey

/**
 * The `SessionTranscript` of an ISO/IEC 18013-5 session (9.1.5.1) after QR engagement ([handover] is [Simple.NULL]) or
 * NFC engagement, as the holder and the reader of this wallet build it: the holder signs its documents over it, and
 * the reader verifies them with it, see [at.asitplus.wallet.app.common.iso.verifier.ProximityResponseValidator].
 *
 * It is built from its parts, as VC-K decodes only the transcript of a QR engagement.
 */
internal fun sessionTranscript(
    encodedDeviceEngagement: ByteArray,
    handover: DataItem,
    eReaderKey: EcPublicKey,
): SessionTranscript = if (handover == Simple.NULL) {
    SessionTranscript.forQr(
        deviceEngagementBytes = encodedDeviceEngagement,
        eReaderKeyBytes = eReaderKey.coseKeyBytes(),
    )
} else {
    SessionTranscript.forNfc(
        deviceEngagementBytes = encodedDeviceEngagement,
        eReaderKeyBytes = eReaderKey.coseKeyBytes(),
        nfcHandover = coseCompliantSerializer.decodeFromByteArray<NFCHandover>(Cbor.encode(handover)),
    )
}

private fun EcPublicKey.coseKeyBytes(): ByteArray =
    coseCompliantSerializer.encodeToByteArray(
        coseCompliantSerializer.decodeFromByteArray<CoseKey>(
            Cbor.encode(toCoseKey().toDataItem())
        )
    )

/**
 * The encoded `SessionTranscript` of a reader session, from which the reader derives the session keys. It is the
 * encoding of [sessionTranscript] of the same parts, as the holder derives the same keys from that.
 */
internal fun encodeReaderSessionTranscript(
    encodedDeviceEngagement: ByteArray,
    handover: DataItem,
    eReaderKey: EcPublicKey
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
