package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class AppSettings internal constructor(
    private val context: Context
) {
    actual fun open() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberAppSettings(): AppSettings {
    return AppSettings(LocalContext.current)
}
