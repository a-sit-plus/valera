package ui.screens

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationPromptContext
import at.asitplus.wallet.app.common.IosCryptoServiceAuthorizationPromptContext

@Composable
actual fun provisioningAuthorizationPromptContext(): CryptoServiceAuthorizationPromptContext {
    return IosCryptoServiceAuthorizationPromptContext()
}