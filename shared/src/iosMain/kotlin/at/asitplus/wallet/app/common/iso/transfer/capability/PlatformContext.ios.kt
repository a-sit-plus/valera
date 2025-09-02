package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable

// iOS doesn't need a context, but we still fulfill the API.
actual class PlatformContext

@Composable
actual fun rememberPlatformContext(): PlatformContext = PlatformContext()