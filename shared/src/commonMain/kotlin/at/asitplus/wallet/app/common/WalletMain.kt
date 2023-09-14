package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService

/**
 * Main class to hold all services needed in the compose app
 */
class WalletMain(
    val objectFactory: ObjectFactory
) {

    suspend fun getCryptoServiceIdentifier(): String {
        return objectFactory.loadCryptoService().identifier
    }

}

interface ObjectFactory {
    suspend fun loadCryptoService(): CryptoService
}