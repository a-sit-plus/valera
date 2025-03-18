package ui.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun RequestBluetoothPermissions(onPermissionsResult: (Boolean) -> Unit)
