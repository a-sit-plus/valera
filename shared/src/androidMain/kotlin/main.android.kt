import androidx.compose.runtime.Composable
import at.asitplus.KmmResult
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService

actual fun getPlatformName(): String = "Android"

@Composable fun MainView() = App(WalletMain(objectFactory = object : ObjectFactory{
    override suspend fun loadCryptoService(): KmmResult<CryptoService> {
        return KmmResult.success(DefaultCryptoService())
    }
}))
