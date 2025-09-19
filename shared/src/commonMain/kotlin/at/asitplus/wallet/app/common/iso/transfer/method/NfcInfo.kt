package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import kotlinx.coroutines.flow.StateFlow

internal expect class NfcInfo(platformContext: PlatformContext, platformAdapter: PlatformAdapter) {
    val isNfcEnabled: StateFlow<Boolean>
    fun openNfcSettings()
}
