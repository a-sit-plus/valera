package data.bletransfer

import android.Manifest
import android.os.Build
import androidx.compose.runtime.*
import com.google.accompanist.permissions.*

@Composable
@OptIn(ExperimentalPermissionsApi::class)
actual fun requestBluetoothPermissions(onPermissionsGranted: (Boolean) -> Unit) {
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    } else {
        // Legacy bluetooth permissions
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        )
    }

    LaunchedEffect(permissionState) {
        permissionState.launchMultiplePermissionRequest()
    }

    when {
        permissionState.allPermissionsGranted -> onPermissionsGranted(true)
        else -> onPermissionsGranted(false)
    }
}
