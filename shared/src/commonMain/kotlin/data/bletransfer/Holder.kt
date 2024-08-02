package data.bletransfer

import androidx.compose.runtime.Composable

expect fun getHolder(): Holder

interface Holder {
    @Composable
    fun getRequirements(check: (Boolean) -> Unit)

    fun hold(updateQrCode: (String) -> Unit)

    fun disconnect()
}