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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.SnackbarService
import at.asitplus.wallet.app.common.WalletMain
import ui.theme.WalletTheme
import view.OnboardingWrapper
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
            if (walletMain.errorService.showError.value == false) {
                OnboardingWrapper(
                    walletMain = walletMain,
                ) {
                    navigator(walletMain)
                }
            } else {
                errorScreen(walletMain)
            }
        }
    }
}



@Composable
fun errorScreen(walletMain: WalletMain) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(10.dp).height(80.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Error",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(walletMain.errorService.throwable.value?.message ?: "Unknown exception", modifier = Modifier.padding(20.dp))
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