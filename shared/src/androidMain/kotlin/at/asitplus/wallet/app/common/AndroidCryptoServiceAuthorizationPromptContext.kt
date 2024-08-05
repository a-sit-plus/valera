package at.asitplus.wallet.app.common

import android.content.Context
import androidx.biometric.BiometricPrompt.PromptInfo

data class AndroidCryptoServiceAuthorizationPromptContext(
    val context: Context,
    val promptInfo: PromptInfo,
) : CryptoServiceAuthorizationPromptContext