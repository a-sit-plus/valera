package at.asitplus.wallet.app.common.iso.transfer.method

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class AppSettings {
    actual fun open(platformContext: PlatformContext) {
        // TODO: check this implementation
        val app = UIApplication.sharedApplication
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        if (app.canOpenURL(url)) app.openURL(url)
    }
}
