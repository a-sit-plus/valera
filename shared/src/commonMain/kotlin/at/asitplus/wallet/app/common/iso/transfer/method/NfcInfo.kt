package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.PlatformAdapter

internal expect class NfcInfo() {
    @Composable
    fun isNfcEnabled(): Boolean
    fun openNfcSettings(platformContext: PlatformContext, platformAdapter: PlatformAdapter)
}
