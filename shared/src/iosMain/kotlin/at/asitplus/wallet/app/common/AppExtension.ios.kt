package at.asitplus.wallet.app.common

import platform.Foundation.NSBundle

internal actual fun isAppExtension(): Boolean = NSBundle.mainBundle.bundlePath.endsWith(".appex")
