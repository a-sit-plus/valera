import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.WalletMain
import navigation.AboutPage
import navigation.CameraPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import ui.theme.WalletTheme
import view.AboutScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.PayloadScreen

var iosTestVar: String = ""
fun iosTest(){
    println("Test variable: $iosTestVar")
}
@Composable
fun App(walletMain: WalletMain) {
    iosTestVar = "123"

    try {
        walletMain.initialize()
    } catch (_: Exception){
        TODO("Display warning screen in case something goes wrong")
    }

    WalletTheme {
        nav(walletMain)
    }
}
var globalBack: () -> Unit = {}

@Composable
fun nav(walletMain: WalletMain) {
    // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer

    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }

    globalBack = { navigationStack.back() }

    AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
        when (page) {
            is HomePage -> {
                HomeScreen( onAbout = { navigationStack.push(AboutPage()) },
                            onCredential = { info ->
                                navigationStack.push(CredentialPage(info))},
                            onScanQrCode = {navigationStack.push(CameraPage())},
                            walletMain)
            }
            is AboutPage -> {
                AboutScreen(walletMain)
            }
            is CredentialPage -> {
                CredentialScreen(id = page.info, walletMain)
            }

            is CameraPage -> {
                CameraView(
                    onFoundPayload = { info ->
                        navigationStack.push(PayloadPage(info))
                    }
                )
            }
            is PayloadPage -> {
                PayloadScreen(text = page.info, onContinueClick = {navigationStack.push(HomePage())}, walletMain)

            }
        }
    }
}

expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme