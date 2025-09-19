package at.asitplus.wallet.app.common.iso.transfer.method

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class PlatformContext internal constructor(val context: Context)

@Composable
actual fun rememberPlatformContext(): PlatformContext =
    PlatformContext(LocalContext.current)
