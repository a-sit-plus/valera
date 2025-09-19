package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter

internal expect class AppSettings(
    platformContext: PlatformContext,
    platformAdapter: PlatformAdapter
) {
    fun open()
}
