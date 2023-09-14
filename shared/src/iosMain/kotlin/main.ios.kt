import androidx.compose.ui.window.ComposeUIViewController
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CryptoService

actual fun getPlatformName(): String = "iOS"

fun MainViewController(objectFactory: ObjectFactory) = ComposeUIViewController { App(WalletMain(objectFactory)) }