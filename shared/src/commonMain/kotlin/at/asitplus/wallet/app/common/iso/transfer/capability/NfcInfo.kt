package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable

expect class NfcInfo() {
    @Composable
    fun isNfcEnabled(): Boolean
    fun openSettings(platformContext: PlatformContext)
}
