package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService
import kotlinx.coroutines.Job

interface WalletCryptoService : CryptoService {
    suspend fun <T> runWithAuthorizationPrompt(
        context: CryptoServiceAuthorizationPromptContext,
        block: suspend WalletCryptoService.() -> T,
    ): T
}