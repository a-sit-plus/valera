package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.runtime.Composable

/**
 * Represents the state of NFC on the device.
 * Provides information about whether NFC is enabled and allows navigating to enable it.
 */
expect class NfcEnabledState {

    /**
     * Indicates if NFC is currently enabled.
     */
    val isEnabled: Boolean

    /**
     * Prompts the user to enable NFC (if possible).
     * On Android this usually opens the NFC settings screen.
     */
    suspend fun enable()
}

/**
 * Remembers and provides the current [NfcEnabledState].
 *
 * If the NFC state changes this will trigger a recomposition.
 * This is useful for the case where the user goes into the settings
 * to manually enable or disable NFC.
 */
@Composable
expect fun rememberNfcEnabledState(): NfcEnabledState
