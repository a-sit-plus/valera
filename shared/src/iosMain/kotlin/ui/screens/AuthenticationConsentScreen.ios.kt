package ui.screens

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import at.asitplus.wallet.app.common.IosCryptoServiceAuthorizationContext
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.button_label_use_device_credential
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import org.jetbrains.compose.resources.stringResource
import platform.LocalAuthentication.LAContext

@Composable
actual fun presentationAuthorizationContext(): CryptoServiceAuthorizationContext {
    return IosCryptoServiceAuthorizationContext(
        contex = LAContext().apply {
            localizedFallbackTitle = stringResource(Res.string.button_label_use_device_credential)
        },
        reason = stringResource(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle),
    )
}