import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.ObjectFactory
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.AppLinkPage
import navigation.CameraPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.theme.WalletTheme
import view.AboutScreen
import view.AppLinkScreen
import view.CameraView
import view.CredentialScreen
import view.HomeScreen
import view.PayloadScreen

var globalBack: () -> Unit = {}

/**
 * Global variable which especially helps to channel information from swift code
 * to compose whenever the app gets called via an associated domain
 */
var appLink = mutableStateOf<String?>(null)

var errorService = ErrorService(mutableStateOf<Boolean>(false), mutableStateOf<String>(""))

@Composable
fun App(walletMain: WalletMain) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(scope, snackbarHostState)

    try {
        walletMain.initialize(snackbarService)
    } catch (e: Exception){
        errorService.emit(e)
    }

    WalletTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { _ ->
            if (errorService.showError.value == false){
                navigator(walletMain)
            } else {
                errorScreen()
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
                                    errorService.emit(e)
                                }
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
                        walletMain = walletMain
                    )

                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun errorScreen(){
    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Error", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer)) {
            Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(100.dp), tint = MaterialTheme.colorScheme.error)
            Text(errorService.errorText.value, modifier = Modifier.padding(20.dp))
            Button(
                modifier = Modifier
                    .padding(vertical = 24.dp),
                onClick = { errorService.reset() }
            ) {
                Text(Resources.BUTTON_CLOSE)
            }
        }
    }

}


expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme