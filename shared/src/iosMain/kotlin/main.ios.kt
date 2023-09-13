import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService

actual fun getPlatformName(): String = "iOS"

fun MainViewController(cryptoService: CryptoService) = ComposeUIViewController { App(WalletMain(cryptoService)) }