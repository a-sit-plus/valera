package data.bletransfer.verifier

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun RequestBluetoothPermissions(onPermissionsResult: (Boolean) -> Unit) {
    val appPermissions: Array<String> =
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    val permissionsNeeded = remember { mutableStateListOf<String>() }
    val activity = LocalContext.current as ComponentActivity

    permissionsNeeded.addAll(appPermissions.filter { permission ->
        ContextCompat.checkSelfPermission(LocalContext.current, permission) != PackageManager.PERMISSION_GRANTED
    })

    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (!allPermissionsGranted) {
            permissions.entries.forEach {
                if (!it.value) {
                    Toast.makeText(activity, "The ${it.key} permission is required for BLE", Toast.LENGTH_LONG).show()
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
