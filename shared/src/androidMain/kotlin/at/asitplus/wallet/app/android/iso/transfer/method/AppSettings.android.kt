package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import at.asitplus.wallet.app.common.PlatformAdapter

actual class AppSettings actual constructor(
    val platformContext: PlatformContext,
    platformAdapter: PlatformAdapter
) {
    actual fun open() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", platformContext.context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        platformContext.context.startActivity(intent)
    }
}
