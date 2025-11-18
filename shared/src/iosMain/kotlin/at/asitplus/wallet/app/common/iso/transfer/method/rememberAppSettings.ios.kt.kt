package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class AppSettings {
    actual fun open() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            UIApplication.sharedApplication.openURL(settingsUrl, mapOf<Any?, Any?>(), null)
        }
    }
}

@Composable
actual fun rememberAppSettings(): AppSettings = AppSettings()
