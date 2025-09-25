package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable

/**
 * Utility to open the app-specific settings screen
 * (used when a permission request has been permanently denied).
 */
expect class AppSettings {
    fun open()
}

@Composable
expect fun rememberAppSettings(): AppSettings
