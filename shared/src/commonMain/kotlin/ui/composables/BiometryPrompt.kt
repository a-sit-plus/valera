package ui.composables

import androidx.compose.runtime.Composable

@Composable
expect fun BiometryPrompt(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
)