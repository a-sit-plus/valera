package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import globalBack
import kotlinx.coroutines.runBlocking

@Composable
fun AboutScreen(onShowLog: () -> Unit, walletMain: WalletMain) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val showAlert = remember { mutableStateOf(false) }
        if (showAlert.value) {
            ResetAlert(showAlert, walletMain)
        }
        var host by remember { mutableStateOf(walletMain.walletConfig.host) }

        Column(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Resources.COMPOSE_WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(Resources.DEMO_APP, fontSize = 24.sp, fontWeight = FontWeight.Bold)


            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Box(
                    Modifier.clip(shape = RoundedCornerShape(10.dp))
                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                        .fillMaxWidth().padding(20.dp)
                ) {
                    Column {
                        Text(Resources.VERSION + ": 0.0.1")
                        Text(Resources.ICONS_FROM + ": icons8.com")
                        Text(Resources.PICTURES_FROM + ": icons8.com")
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Issuing Service") },
                            modifier = Modifier.padding(vertical = 20.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                }
            }



            Row(modifier = Modifier.padding(vertical = 24.dp)) {
                Button(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = { showAlert.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(Resources.RESET_APP)
                }
                Button(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = {
                        onShowLog()
                    }
                ) {
                    Text(Resources.BUTTON_SHOW_LOG)
                }
            }
            Button(
                modifier = Modifier
                    .padding(vertical = 24.dp),
                onClick = {
                    walletMain.walletConfig.host = host
                    walletMain.walletConfig.exportConfig()
                    globalBack()
                }
            ) {
                Text(Resources.BUTTON_CLOSE)
            }
        }
    }
}

@Composable
fun ResetAlert(showAlert: MutableState<Boolean>, walletMain: WalletMain) {
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
                    walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
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