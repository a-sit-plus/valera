import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.AppLinkPage
import navigation.CameraPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ui.theme.WalletTheme
import view.AboutScreen
import view.AppLinkScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.LogScreen
import view.PayloadScreen


/**
 * Global variable to utilize back navigation functionality
 */
var globalBack: () -> Unit = {}

/**
 * Global variable which especially helps to channel information from swift code
 * to compose whenever the app gets called via an associated domain
 */
var appLink = mutableStateOf<String?>(null)


/**
 * Global variable to test at least something from the iOS UITest
 */
var iosTestValue = Resources.IOS_TEST_VALUE

@Composable
fun App(walletMain: WalletMain) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Exception){
        walletMain.errorService.emit(e)
    }

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { _ ->
            if (walletMain.errorService.showError.value == false){
                navigator(walletMain)
            } else {
                errorScreen(walletMain)
            }

        }
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
                        onLoginWithIdAustria = {
                            CoroutineScope(Dispatchers.Default).launch {
                                try {
                                    walletMain.provisioningService.startProvisioning()
                                } catch (e: Exception) {
                                    walletMain.errorService.emit(e)
                                }
                            }
                        },
                        walletMain = walletMain
                    )
                }

                is AboutPage -> {
                    AboutScreen(
                        onShowLog = {navigationStack.push(LogPage())},
                        walletMain)
                }

                is LogPage -> {
                    LogScreen()
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
                        walletMain = walletMain
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun errorScreen(walletMain: WalletMain){
    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Error", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer)) {
            Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(100.dp), tint = MaterialTheme.colorScheme.error)
            Text(walletMain.errorService.errorText.value, modifier = Modifier.padding(20.dp))
            Button(
                modifier = Modifier
                    .padding(vertical = 24.dp),
                onClick = { walletMain.errorService.reset() }
            ) {
                Text(Resources.BUTTON_CLOSE)
            }
        }
    }

}


expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme