package ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext

@Composable
actual fun BiometryPrompt(
    authorizationContext: CryptoServiceAuthorizationContext,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
) {
    LaunchedEffect(true) {
        // This is now directly implemented in the android crypto service
        onSuccess(BiometryPromptSuccessResult())
    }
}