import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.wallet.app.common.ProvisioningClient
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.AppLinkPage
import navigation.CameraPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import ui.theme.WalletTheme
import view.AboutScreen
import view.AppLinkScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.PayloadScreen

var globalBack: () -> Unit = {}

var appLink = mutableStateOf<String?>(null)

@Composable
fun App(walletMain: WalletMain) {
    try {
        walletMain.initialize()
    } catch (_: Exception){
        TODO("Display warning screen in case something goes wrong")
    }

    WalletTheme {
        navigator(walletMain)
    }
}

@Composable
fun navigator(walletMain: WalletMain) {
    key(appLink.value) {
        val defaultPage: Page
        if (appLink.value == null) {
            defaultPage = HomePage()
        } else {
            defaultPage = AppLinkPage()
        }

        // Modified from https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer
        val navigationStack = rememberSaveable(
            saver = listSaver(
                restore = { NavigationStack(*it.toTypedArray()) },
                save = { it.stack },
            )
        ) {
            NavigationStack(defaultPage)
        }

        globalBack = { navigationStack.back() }

        AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
            when (page) {
                is HomePage -> {
                    HomeScreen(
                        onAbout = { navigationStack.push(AboutPage()) },
                        onCredential = { info ->
                            navigationStack.push(CredentialPage(info))
                        },
                        onScanQrCode = { navigationStack.push(CameraPage()) },
                        onOidc = {
                            CoroutineScope(Dispatchers.Default).launch {
                                val client = ProvisioningClient()
                                client.test()
                            }
                        },
                        walletMain = walletMain
                    )
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
                    PayloadScreen(
                        text = page.info,
                        onContinueClick = { navigationStack.push(HomePage()) },
                        walletMain
                    )

                }

                is AppLinkPage -> {
                    AppLinkScreen(
                        onContinueClick = { navigationStack.push(HomePage()) }
                    )

                }
            }
        }
    }
}


expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme