package at.asitplus.wallet.app.common.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.DataItem
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.mdoc.transport.MdocTransport
import kotlin.time.Duration

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp

/**
 * A [PresentmentMechanism] to use with [PresentmentModel] for ISO/IEC 18013-5:2021 proximity presentations.
 *
 * @property transport a [MdocTransport] connected to the remote mdoc reader.
 * @property ephemeralDeviceKey the ephemeral device key for the session.
 * @property encodedDeviceEngagement the Device Engagement.
 * @property handover the handover.
 * @property engagementDuration the time engagement took if known, `null` otherwise.
 * @property allowMultipleRequests if true, multiple requests are allowed.
 */
class MdocPresentmentMechanism(
    val transport: MdocTransport,
    val ephemeralDeviceKey: EcPrivateKey,
    val encodedDeviceEngagement: ByteString,
    val handover: DataItem,
    val engagementDuration: Duration?,
    val allowMultipleRequests: Boolean
) : PresentmentMechanism {

    override fun close() {
        CoroutineScope(Dispatchers.IO).launch { transport.close() }
    }
}