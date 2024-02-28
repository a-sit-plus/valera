package ui.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun BiometryScanner(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text("Missing BiometryScanner Implementation")
}