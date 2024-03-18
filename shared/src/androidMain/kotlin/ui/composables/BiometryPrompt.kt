@file:OptIn(ExperimentalResourceApi::class)

package ui.composables

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
import composewalletapp.shared.generated.resources.BUTTON_LABEL_CANCEL
import composewalletapp.shared.generated.resources.BUTTON_LABEL_CONFIRM
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_HW_UNAVAILABLE
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_NONE_ENROLLED
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_NO_HARDWARE
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_UNKNOWN
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_ERROR_UNSUPPORTED
import composewalletapp.shared.generated.resources.ERROR_BIOMETRIC_STATUS_UNKNOWN
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.WARNING
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun BiometryPrompt(
    title: String,
    subtitle: String,
    onSuccess: (BiometryPromptSuccessResult) -> Unit,
    onDismiss: (BiometryPromptDismissResult) -> Unit,
) {
    // source: https://medium.com/@muaz.kadan/biometric-authentication-in-jetpack-compose-5e4d7e35d1e7
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
                        Napier.d("Authentication failed with error code $errorCode: $errString")
                        onDismiss(BiometryPromptDismissResult(
                            errorCode = errorCode,
                            errorString = errString.toString(),
                        ))
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Napier.d("Authentication succeeded")
                        onSuccess(BiometryPromptSuccessResult())
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(stringResource(Res.string.BUTTON_LABEL_CANCEL))
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        else -> {
            val text = when (isBiometricAvailable) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    // No biometric features available on this device
                    stringResource(Res.string.ERROR_BIOMETRIC_ERROR_NO_HARDWARE)
                }

                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    // Biometric features are currently unavailable.
                    stringResource(Res.string.ERROR_BIOMETRIC_ERROR_HW_UNAVAILABLE)
                }

                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    // Biometric features available but a security vulnerability has been discovered
                    stringResource(Res.string.ERROR_BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED)
                }

                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    // Biometric features are currently unavailable because the specified options are incompatible with the current Android version..
                    stringResource(Res.string.ERROR_BIOMETRIC_ERROR_UNSUPPORTED)
                }

                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    // Unable to determine whether the user can authenticate using biometrics
                    stringResource(Res.string.ERROR_BIOMETRIC_STATUS_UNKNOWN)
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    // The user can't authenticate because no biometric or device credential is enrolled.
                    stringResource(Res.string.ERROR_BIOMETRIC_ERROR_NONE_ENROLLED)
                }

                else -> stringResource(Res.string.ERROR_BIOMETRIC_ERROR_UNKNOWN)
            }
            AlertDialog(
                title = {
                    Text(stringResource(Res.string.WARNING))
                },
                text = {
                    Text(text)
                },
                onDismissRequest = {},
                dismissButton = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismiss(BiometryPromptDismissResult(
                                errorCode = -1,
                                errorString = text,
                            ))
                        },
                    ) {
                        Text(stringResource(Res.string.BUTTON_LABEL_CONFIRM))
                    }
                },
            )
        }
    }
}