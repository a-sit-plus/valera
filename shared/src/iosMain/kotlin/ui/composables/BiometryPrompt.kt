package ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
) {
    LaunchedEffect(true) {
        onSuccess(BiometryPromptSuccessResult())
    }
}