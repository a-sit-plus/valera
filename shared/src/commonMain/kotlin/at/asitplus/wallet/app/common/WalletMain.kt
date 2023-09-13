package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.CryptoService

class WalletMain(
    val cryptoService: CryptoService,
) {

    val cryptoServiceIdentifier by lazy {
        cryptoService.identifier
    }

}