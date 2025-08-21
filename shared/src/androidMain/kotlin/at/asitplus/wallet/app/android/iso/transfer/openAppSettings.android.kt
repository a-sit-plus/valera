package at.asitplus.wallet.app.common.iso.transfer

import android.content.Intent
import android.net.Uri
import android.provider.Settings

actual fun openAppSettings(platformContext: PlatformContext) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", platformContext.context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    platformContext.context.startActivity(intent)
}
