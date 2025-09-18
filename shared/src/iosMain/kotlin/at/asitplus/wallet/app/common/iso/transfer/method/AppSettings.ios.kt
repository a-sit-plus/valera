package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter

actual class AppSettings {
    actual fun open(platformContext: PlatformContext, platformAdapter: PlatformAdapter) {
        // TODO: check this implementation
        platformAdapter.openUrl("prefs:root=General")
    }
}
