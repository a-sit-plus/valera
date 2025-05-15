package at.asitplus.wallet.app.common.iso.transfer

import androidx.compose.runtime.Composable

expect class NfcInfo() {
    @Composable
    fun isNfcEnabled(): Boolean
}
