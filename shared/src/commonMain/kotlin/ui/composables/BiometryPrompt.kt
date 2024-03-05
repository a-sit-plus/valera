package ui.composables

import androidx.compose.runtime.Composable

data class BiometryPromptDismissResult(
    val errorCode: Int,
    val errorString: String,
)

class BiometryPromptSuccessResult

@Composable
expect fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
)