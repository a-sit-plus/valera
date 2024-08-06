package ui.screens

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import at.asitplus.wallet.app.common.AndroidCryptoServiceAuthorizationPromptContext
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationPromptContext
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_to_load_data_subtitle
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_to_load_data_title
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_subtitle
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_to_bind_credentials_title
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import composewalletapp.shared.generated.resources.snackbar_credential_loaded_successfully
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.composables.BiometryPrompt
import ui.composables.buttons.NavigateUpButton

@Composable
actual fun provisioningAuthorizationPromptContext(): CryptoServiceAuthorizationPromptContext {
    return AndroidCryptoServiceAuthorizationPromptContext(
        context = LocalContext.current,
        promptInfo = BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(stringResource(Res.string.biometric_authentication_prompt_to_bind_credentials_title))
            setTitle(stringResource(Res.string.biometric_authentication_prompt_to_bind_credentials_subtitle))
            setAllowedAuthenticators(
                when (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    true -> BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                    false -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                }
            )
        }.build()
    )
}