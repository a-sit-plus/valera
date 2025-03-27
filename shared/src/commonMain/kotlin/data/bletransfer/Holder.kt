package data.bletransfer

import androidx.compose.runtime.Composable
import at.asitplus.wallet.lib.iso.Document
import data.bletransfer.util.RequestedDocument

expect fun getHolder(): Holder

interface Holder {
    @Composable
    fun getRequirements(check: (Boolean) -> Unit)

    fun getAttributes(): List<RequestedDocument>

    fun hold(updateQrCode: (String) -> Unit, onRequestedAttributes: () -> Unit)

    fun disconnect()

    fun send(credentials: List<Document>, launchAfterSuccessfulSend: () -> Unit)

    fun getRequesterIdentity(): Map<String, String>
    fun getRequesterVerified(): Boolean
}
