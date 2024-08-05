package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import kotlinx.coroutines.Job

interface WalletCryptoService : CryptoService {
    suspend fun runWithAuthorizationPrompt(
        context: CryptoServiceAuthorizationPromptContext,
        block: suspend WalletCryptoService.() -> Unit,
    ): Job
}