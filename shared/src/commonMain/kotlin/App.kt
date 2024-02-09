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
import androidx.compose.runtime.LaunchedEffect
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
import io.ktor.http.parseQueryString
import kotlinx.coroutines.launch
import navigation.AboutPage
import navigation.CameraPage
import navigation.ConsentPage
import navigation.CredentialPage
import navigation.HomePage
import navigation.LoadingPage
import navigation.LogPage
import navigation.NavigationStack
import navigation.Page
import navigation.PayloadPage
import ui.theme.WalletTheme
import view.AboutScreen
import view.CameraView
import view.ConsentScreen
import view.CredentialScreen
import view.HomeScreen
import view.LoadingScreen
import view.LogScreen
import view.PayloadScreen
import view.errorScreen


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
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService = SnackbarService(walletMain.scope, snackbarHostState)

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
            if (!walletMain.errorService.showError.value){
                navigator(walletMain)
            } else {
                errorScreen(walletMain)
            }

        }
    }
}


expect fun getPlatformName(): String

@Composable
expect fun getColorScheme(): ColorScheme