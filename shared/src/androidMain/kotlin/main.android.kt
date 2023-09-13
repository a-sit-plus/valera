import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.DefaultCryptoService

actual fun getPlatformName(): String = "Android"

@Composable fun MainView() = App(WalletMain(DefaultCryptoService()))
