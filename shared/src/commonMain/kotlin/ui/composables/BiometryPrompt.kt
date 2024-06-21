package ui.composables

import androidx.compose.runtime.Composable

data class BiometryPromptDismissResult(
    val errorCode: Int,
    val errorString: String,
)

class BiometryPromptSuccessResult

fun interface BiometryPrompt {
    @Composable
    operator fun invoke(
        title: String,
        subtitle: String,
        onSuccess: (BiometryPromptSuccessResult) -> Unit,
        onDismiss: (BiometryPromptDismissResult) -> Unit,
    )
}