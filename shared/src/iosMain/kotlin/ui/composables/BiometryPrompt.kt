package ui.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text("Missing BiometryScanner Implementation")
}