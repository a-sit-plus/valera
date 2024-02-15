package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appLink
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun AppLinkScreen(walletMain: WalletMain){
    val host by rememberSaveable {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.host.first())
        }
    }

    LaunchedEffect(true) {
        Napier.d("Redirect: ${appLink.value}")
    }
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Resources.COMPOSE_WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        CircularProgressIndicator(
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        runBlocking {
            if (appLink.value?.contains("$host/m1/login/oauth2/code/idaq?code=") == true) {
                try {
                    walletMain.provisioningService.handleResponse(appLink.value.toString())
                    walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
                } catch (e: Exception) {
                    walletMain.errorService.emit(e)
                }

            }
            if (appLink.value?.contains("$host/mobile") == true) {
                try {
                    walletMain.presentationService.startSiop(appLink.value.toString())
                } catch (e: Exception) {
                    walletMain.errorService.emit(e)
                }
            }
        }

        appLink.value = null
    }
}