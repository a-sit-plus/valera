import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import io.ktor.http.ContentType
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
import kotlin.reflect.typeOf


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
    } catch (e: Throwable){
        walletMain.errorService.emit(UncorrectableErrorException(e))
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
                                } catch (e: Throwable) {
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
                    LogScreen(walletMain)
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
    val throwable = walletMain.errorService.throwable.value
    val message = throwable?.message ?: "Unknown Message"
    val cause = throwable?.cause?.message ?: "Unknown Cause"
    val tint: Color
    var onButton: () -> Unit
    var buttonText: String
    if(throwable?.message == "UncorrectableErrorException") {
        tint = Color.Red
        buttonText = Resources.BUTTON_EXIT_APP
        onButton = { walletMain.platformAdapter.exitApp() }
    } else{
        tint = Color(255,210,0)
        buttonText = Resources.BUTTON_CLOSE
        onButton = { walletMain.errorService.reset() }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Error", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer).padding(bottom = 80.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, Modifier.size(100.dp), tint = tint)
            Text("Message:", fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.heightIn(max = 150.dp).background(color = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(message, modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp).fillMaxWidth().verticalScroll(rememberScrollState()), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.size(5.dp))
            Text("Cause:", fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.heightIn(max = 150.dp).background(color = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(cause, modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp).fillMaxWidth().verticalScroll(rememberScrollState()), textAlign = TextAlign.Center)
            }
        }
    }
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()){
        Column(modifier = Modifier.height(80.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Button(
                onClick = onButton
            ) {
                Text(buttonText)
            }
        }
    }
}


expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme