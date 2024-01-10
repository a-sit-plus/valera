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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appLink
import at.asitplus.wallet.app.common.HOST
import at.asitplus.wallet.app.common.WalletMain
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking

@Composable
fun AppLinkScreen(walletMain: WalletMain){
    Napier.d("Redirect: ${appLink.value}")
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
            if (appLink.value?.contains("$HOST/m1/login/oauth2/code/idaq?code=") == true) {
                walletMain.provisioningService.handleResponse(appLink.value.toString())
                walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY)
            }
            if (appLink.value?.contains("$HOST/mobile") == true) {
                walletMain.presentationService.startSiop(appLink.value.toString())
            }
        }

        appLink.value = null
    }
}