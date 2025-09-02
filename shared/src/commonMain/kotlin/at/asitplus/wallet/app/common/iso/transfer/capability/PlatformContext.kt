package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.runtime.Composable

expect class PlatformContext

@Composable
expect fun rememberPlatformContext(): PlatformContext
