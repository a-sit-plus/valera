package ui.permissions

@Composable
expect fun RequestBluetoothPermissions(onPermissionsResult: (Boolean) -> Unit)