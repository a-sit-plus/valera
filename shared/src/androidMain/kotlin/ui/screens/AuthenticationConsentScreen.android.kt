package ui.screens

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.AndroidCryptoServiceAuthorizationPromptContext
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationPromptContext
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import org.jetbrains.compose.resources.stringResource


@Composable
actual fun presentationAuthorizationPromptContext(): CryptoServiceAuthorizationPromptContext {
    return AndroidCryptoServiceAuthorizationPromptContext(
        context = LocalContext.current,
        promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(stringResource(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title))
            setSubtitle(stringResource(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle))
            setAllowedAuthenticators(
                when (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    true -> BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                    false -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                }
            )
        }.build()
    )
}