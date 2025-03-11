package data.bletransfer

import androidx.compose.runtime.Composable

@Composable
expect fun requestBluetoothPermissions(onPermissionsGranted: (Boolean) -> Unit)
