package at.asitplus.wallet.app.common.iso.transfer

import kotlinx.io.bytestring.ByteString
import org.multipaz.util.toBase64Url

object MdocHelper {
    /**
     * Builds a device engagement QR code string with the MDOC prefix.
     *
     * @param data The raw engagement data as ByteString.
     * @return The device engagement QR code as String
     */
    fun buildDeviceEngagementQrCode(data: ByteString): String {
        return MdocConstants.MDOC_PREFIX + data.toByteArray().toBase64Url()
    }
}
