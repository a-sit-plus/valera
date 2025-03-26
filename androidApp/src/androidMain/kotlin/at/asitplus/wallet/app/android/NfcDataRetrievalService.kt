package at.asitplus.wallet.app.android

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import org.multipaz.mdoc.transport.NfcTransportMdoc
import io.github.aakira.napier.Napier

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp
class NfcDataRetrievalService: HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        Napier.i("NfcDataRetrievalService: Processing APDU")
        try {
            NfcTransportMdoc.processCommandApdu(
                commandApdu = commandApdu,
                sendResponse = { responseApdu -> sendResponseApdu(responseApdu) }
            )
        } catch (e: Throwable) {
            Napier.e("NfcDataRetrievalService: Failed to process APDU", e)
        }
        return null
    }

    override fun onDeactivated(reason: Int) {
        Napier.i( "NfcDataRetrievalService: Deactivation event received because of $reason")
        NfcTransportMdoc.onDeactivated()
    }
}