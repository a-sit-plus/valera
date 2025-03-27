package at.asitplus.wallet.app.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun RequestBluetoothPermissions(onPermissionsResult: (Boolean) -> Unit, showError: (String) -> Unit) {
    val appPermissions: Array<String> =
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    val permissionsNeeded = remember { mutableStateListOf<String>() }

    permissionsNeeded.addAll(appPermissions.filter { permission ->
        ContextCompat.checkSelfPermission(LocalContext.current, permission) != PackageManager.PERMISSION_GRANTED
    })



    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (!allPermissionsGranted) {
            permissions.entries.forEach {
                if (!it.value) {
                    showError("The ${it.key} permission is required for BLE")
                }
            }
        }
        onPermissionsResult(allPermissionsGranted)
    }

    LaunchedEffect(permissionsNeeded) {
        if (permissionsNeeded.isNotEmpty()) {
            permissionsLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            onPermissionsResult(true)
        }
    }
}
