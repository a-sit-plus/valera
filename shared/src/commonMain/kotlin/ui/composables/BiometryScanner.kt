package ui.composables

import androidx.compose.runtime.Composable

@Composable
expect fun BiometryScanner(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
)