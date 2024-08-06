package ui.composables

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_confirm
import composewalletapp.shared.generated.resources.error_biometric_error_hardware_unavailable
import composewalletapp.shared.generated.resources.error_biometric_error_no_hardware
import composewalletapp.shared.generated.resources.error_biometric_error_none_enrolled
import composewalletapp.shared.generated.resources.error_biometric_error_security_update_required
import composewalletapp.shared.generated.resources.error_biometric_error_unknown
import composewalletapp.shared.generated.resources.error_biometric_error_unsupported
import composewalletapp.shared.generated.resources.error_biometric_status_unknown
import composewalletapp.shared.generated.resources.warning
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
) {
    LaunchedEffect(true) {
        // This is now directly implemented in the android crypto service
        onSuccess(BiometryPromptSuccessResult())
    }
}