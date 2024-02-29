package ui.composables

import androidx.compose.runtime.Composable

@Composable
expect fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
)