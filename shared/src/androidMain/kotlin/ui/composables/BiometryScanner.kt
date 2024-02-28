package ui.composables

import Resources
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
actual fun BiometryScanner(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }

    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    }
    when (isBiometricAvailable) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val executor = remember { ContextCompat.getMainExecutor(context) }
            val biometricPrompt = BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onDismiss()
                        // handle authentication error here
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                        // handle authentication success here
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setTitle("Biometric Authentication")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use password")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        else -> {
            AlertDialog(
                title = {
                    Text(Resources.WARNING)
                },
                text = {
                    when (isBiometricAvailable) {
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            // No biometric features available on this device
                            Text("BIOMETRIC_ERROR_NO_HARDWARE")
                        }

                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                            // Biometric features are currently unavailable.
                            Text("BIOMETRIC_ERROR_HW_UNAVAILABLE")
                        }

                        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                            // Biometric features available but a security vulnerability has been discovered
                            Text("BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
                        }

                        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                            // Biometric features are currently unavailable because the specified options are incompatible with the current Android version..
                            Text("BIOMETRIC_ERROR_UNSUPPORTED")
                        }

                        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                            // Unable to determine whether the user can authenticate using biometrics
                            Text("BIOMETRIC_STATUS_UNKNOWN")
                        }

                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                            // The user can't authenticate because no biometric or device credential is enrolled.
                            Text("BIOMETRIC_ERROR_NONE_ENROLLED")
                        }
                    }
                },
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text(Resources.BUTTON_CONFIRM)
                    }
                },
            )
        }
    }
}