package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter

internal expect class AppSettings() {
    fun open(platformContext: PlatformContext, platformAdapter: PlatformAdapter)
}
