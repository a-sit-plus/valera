package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.CryptoService

/**
 * Main class to hold all services needed in the Compose App.
 */
class WalletMain(
    val objectFactory: ObjectFactory
) {

    suspend fun getCryptoServiceIdentifier(): String {
        val cryptoService = objectFactory.loadCryptoService().getOrElse {
            return "null"
        }
        return cryptoService.identifier
    }

}

/**
 * Factory to call back to native code to create service objects needed in [WalletMain].
 *
 * Especially useful to call back to Swift code, i.e. to create a [CryptoService] based
 * on Apple's CryptoKit.
 *
 * Most methods are suspending to be able to use biometric authentication or show some other
 * dialogs. Also return `KmmResult` to be able to transport exceptions across system boundaries
 * efficiently.
 */
interface ObjectFactory {
    suspend fun loadCryptoService(): KmmResult<CryptoService>
}