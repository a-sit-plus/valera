package at.asitplus.wallet.app.common.iso.transfer.method

// This is copied from Multipaz repository until there is a release including this
// org.multipaz.compose.permissions
// https://github.com/openwallet-foundation/multipaz/blob/main/multipaz-compose/src/commonMain/kotlin/org/multipaz/compose/permissions/rememberBluetoothEnabledState.kt

import androidx.compose.runtime.Composable

/**
 * Represents the state of Bluetooth on the device.
 * Provides information about whether Bluetooth is enabled and allows enabling it.
 */
expect class BluetoothEnabledState {

    /**
     * Indicates if Bluetooth is currently enabled.
     */
    val isEnabled: Boolean

    /**
     * Enables Bluetooth on the device.
     */
    suspend fun enable()
}

/**
 * Remembers and provides the current [BluetoothEnabledState].
 *
 * If the bluetooth state changes this will trigger a recomposition.
 * This is useful for the case where the user goes into the settings or
 * notification drawer to manually grant or deny the permission.
 */
@Composable
expect fun rememberBluetoothEnabledState(): BluetoothEnabledState