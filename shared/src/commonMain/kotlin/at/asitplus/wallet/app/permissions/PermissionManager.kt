package at.asitplus.wallet.app.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun RequestBluetoothPermissions(onPermissionsResult: (Boolean) -> Unit, showError: (String) -> Unit)
