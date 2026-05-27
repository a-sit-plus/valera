package at.asitplus.wallet.app.android

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import at.asitplus.wallet.app.common.presentation.NfcTransferState
import org.multipaz.mdoc.transport.NfcTransportMdoc
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.multipaz.nfc.CommandApdu

// Based on the identity-credential sample code
// https://github.com/openwallet-foundation-labs/identity-credential/tree/main/samples/testapp
class NfcDataRetrievalService: HostApduService() {
    private val responseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        runCatching { CommandApdu.decode(commandApdu) }
            .onSuccess { command ->
                Napier.i(
                    "NfcDataRetrievalService: APDU cla=${command.cla}, ins=${command.ins}, " +
                            "p1=${command.p1}, p2=${command.p2}, payload=${command.payload.size}, le=${command.le}"
                )
            }
            .onFailure {
                Napier.w("NfcDataRetrievalService: Could not decode APDU of ${commandApdu.size} bytes", it)
            }
        try {
            NfcTransportMdoc.processCommandApdu(
                commandApdu = commandApdu,
                sendResponse = { responseApdu ->
                    Napier.i("NfcDataRetrievalService: Sending response APDU bytes=${responseApdu.size}")
                    responseScope.launch {
                        sendResponseApdu(responseApdu)
                    }
                }
            )
        } catch (e: Throwable) {
            Napier.e("NfcDataRetrievalService: Failed to process APDU", e)
        }
        return null
    }

    override fun onDeactivated(reason: Int) {
        Napier.i( "NfcDataRetrievalService: Deactivation event received because of $reason")
        NfcTransportMdoc.onDeactivated()
        NfcTransferState.nfcDataTransferActive.value = false
    }

    override fun onDestroy() {
        responseScope.cancel()
        super.onDestroy()
    }
}
