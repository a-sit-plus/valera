package at.asitplus.wallet.app.common.iso.transfer.method

import at.asitplus.wallet.app.common.PlatformAdapter
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class AppSettings {
    actual fun open(platformContext: PlatformContext, platformAdapter: PlatformAdapter) {
        // TODO: check this implementation
        platformAdapter.openUrl(UIApplicationOpenSettingsURLString)
    }
}
