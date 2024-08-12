package ui.composables

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext

data class BiometryPromptDismissResult(
    val errorCode: Int,
    val errorString: String,
)

class BiometryPromptSuccessResult

@Composable
expect fun BiometryPrompt(
    authorizationContext: CryptoServiceAuthorizationContext,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
)