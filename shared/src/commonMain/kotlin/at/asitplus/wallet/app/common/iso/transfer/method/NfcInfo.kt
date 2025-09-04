package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable

internal expect class NfcInfo() {
    @Composable
    fun isNfcEnabled(): Boolean
    fun openNfcSettings(platformContext: PlatformContext)
}
