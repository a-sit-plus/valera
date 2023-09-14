package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService

/**
 * Main class to hold all services needed in the compose app
 */
class WalletMain(
    val cryptoService: CryptoService,
) {

    val cryptoServiceIdentifier by lazy {
        cryptoService.identifier
    }

}