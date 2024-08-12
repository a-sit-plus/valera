package ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.IosCryptoServiceAuthorizationContext
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

@Composable
actual fun BiometryPrompt(
    authorizationContext: CryptoServiceAuthorizationContext,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
) {
    LaunchedEffect(true) {
        // Done in iOS crypto service now
//        onSuccess(BiometryPromptSuccessResult())
        if(authorizationContext !is IosCryptoServiceAuthorizationContext) {
            throw IllegalArgumentException("authorizationContext")
        }
        authorizationContext.contex.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = authorizationContext.reason) { boolResult, nsError ->
            if (boolResult) {
                onSuccess(BiometryPromptSuccessResult())
            } else {
                onDismiss(BiometryPromptDismissResult(nsError?.code?.toInt() ?: -1, nsError?.localizedDescription ?: "unknown"))
            }
        }
    }
}