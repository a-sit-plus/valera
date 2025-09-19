package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class AppSettings actual constructor(
    platformContext: PlatformContext,
    val platformAdapter: PlatformAdapter
) {
    // Jump to app settings where the permissions can be managed for the actual app
    actual fun open() {
        platformAdapter.openUrl(UIApplicationOpenSettingsURLString)
    }
}
