import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService

actual fun getPlatformName(): String = "iOS"

fun MainViewController(cryptoServiceSupplier: ()->CryptoService) = ComposeUIViewController { App(WalletMain(cryptoServiceSupplier)) }