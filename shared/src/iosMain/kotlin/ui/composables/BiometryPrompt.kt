package ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

object IosBiometryPrompt : BiometryPrompt {
    @Composable
    override operator fun invoke(
        title: String,
        subtitle: String,
        onSuccess: (BiometryPromptSuccessResult) -> Unit,
        onDismiss: (BiometryPromptDismissResult) -> Unit,
    ) {
        LaunchedEffect(true) {
            LAContext().apply { localizedFallbackTitle = "Passcode" }.evaluatePolicy(
                LAPolicyDeviceOwnerAuthentication, localizedReason = title
            ) { boolResult, nsError ->
                if (boolResult) {
                    onSuccess(BiometryPromptSuccessResult())
                } else {
                    onDismiss(
                        BiometryPromptDismissResult(
                            nsError?.code?.toInt() ?: -1, nsError?.localizedDescription ?: "unknown"
                        )
                    )
                }
            }
        }
    }
}