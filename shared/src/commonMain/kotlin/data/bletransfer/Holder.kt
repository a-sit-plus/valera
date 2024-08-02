package data.bletransfer

import androidx.compose.runtime.Composable
import data.bletransfer.verifier.ReceivedDocument

expect fun getHolder(): Holder

interface Holder {
    @Composable
    fun getRequirements(check: (Boolean) -> Unit)

    fun hold(updateQrCode: (String) -> Unit, updateRequestedAttributes: (List<ReceivedDocument>) -> Unit)

    fun disconnect()
}