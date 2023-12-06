package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import globalBack
import kotlinx.coroutines.runBlocking

@Composable
fun AboutScreen(walletMain: WalletMain){
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val showAlert = remember { mutableStateOf(false) }
        if (showAlert.value) {
            ResetAlert(showAlert, walletMain)
        }
        Text(Resources.COMPOSE_WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(Resources.DEMO_APP, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)


        Box(Modifier.fillMaxWidth().padding(20.dp)){
            Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).fillMaxWidth().padding(20.dp)){
                Column {
                    Text(Resources.VERSION + " : 0.0.1")
                    Text(Resources.ICONS_FROM + " : icons8.com")
                    Text(Resources.PICTURES_FROM+ " : icons8.com")
                }

            }
        }
        Button(
            modifier = Modifier
                .padding(vertical = 24.dp),
            onClick = { globalBack() }
        ) {
            Text(Resources.BUTTON_CLOSE)
        }
        Button(
            modifier = Modifier
                .padding(vertical = 24.dp),
            onClick = { showAlert.value = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text(Resources.RESET_APP)
        }
    }
}

@Composable
fun ResetAlert(showAlert: MutableState<Boolean>, walletMain: WalletMain){
    AlertDialog(
        title = {
            Text(Resources.WARNING)
        },
        text = {
            Text(Resources.RESET_APP_ALERT_TEXT)
        },
        onDismissRequest = {
            showAlert.value = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runBlocking { walletMain.resetApp() }
                    walletMain.snackbarService.showSnackbar("Reset App successfully")
                    showAlert.value = false
                }
            ) {
                Text(Resources.BUTTON_CONFIRM)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showAlert.value = false
                }
            ) {
                Text(Resources.BUTTON_DISMISS)
            }
        }
    )
}