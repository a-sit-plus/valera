package data.bletransfer

import androidx.compose.runtime.Composable
import data.bletransfer.holder.RequestedDocument

expect fun getHolder(): Holder

interface Holder {
    @Composable
    fun getRequirements(check: (Boolean) -> Unit)

    fun getAttributes(): List<RequestedDocument>

    fun hold(updateQrCode: (String) -> Unit, onRequestedAttributes: () -> Unit)

    fun disconnect()
}