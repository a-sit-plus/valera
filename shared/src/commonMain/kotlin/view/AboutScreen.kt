package view

import Resources
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import at.asitplus.wallet.lib.data.ConstantIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigateUp: () -> Unit,
    onShowLog: () -> Unit,
    walletMain: WalletMain,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val showAlert = remember { mutableStateOf(false) }
        if (showAlert.value) {
            ResetAlert(
                showAlert,
                onDismissRequest = {
                    showAlert.value = false
                },
                onResetConfirm = {
                    runBlocking { walletMain.resetApp() }
                    navigateUp()
                    walletMain.snackbarService.showSnackbar(Resources.SNACKBAR_RESET_APP_SUCCESSFULLY)
                },
            )
        }
        var credentialRepresentation by remember {
            runBlocking {
                mutableStateOf(walletMain.walletConfig.credentialRepresentation.first())
            }
        }
        var host by rememberSaveable {
            runBlocking {
                mutableStateOf(walletMain.walletConfig.host.first())
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                Resources.COMPOSE_WALLET,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                Resources.DEMO_APP,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )


            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Box(
                    Modifier.clip(shape = RoundedCornerShape(10.dp))
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
                        var showMenu by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = showMenu,
                            onExpandedChange = { showMenu = !showMenu }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(),
                                readOnly = true,
                                value = credentialRepresentation.name,
                                onValueChange = {},
                                label = { Text("Credential Representation") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) },
                            )
                            ExposedDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = {
                                    showMenu = false
                                },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("PLAIN_JWT") },
                                    onClick = {
                                        credentialRepresentation =
                                            ConstantIndex.CredentialRepresentation.PLAIN_JWT
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("SD_JWT") },
                                    onClick = {
                                        credentialRepresentation =
                                            ConstantIndex.CredentialRepresentation.SD_JWT
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ISO_MDOC") },
                                    onClick = {
                                        credentialRepresentation =
                                            ConstantIndex.CredentialRepresentation.ISO_MDOC
                                        showMenu = false
                                    }
                                )
                            }
                        }
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
                    walletMain.walletConfig.set(
                        host = host,
                        credentialRepresentation = credentialRepresentation,
                    )
                    navigateUp()
                }
            ) {
                Text(Resources.BUTTON_LABEL_SAVE)
            }
        }
    }
}


@Composable
fun ResetAlert(
    showAlert: MutableState<Boolean>,
    onDismissRequest: () -> Unit,
    onResetConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(Resources.WARNING)
        },
        text = {
            Text(Resources.RESET_APP_ALERT_TEXT)
        },
        onDismissRequest = onDismissRequest,
//        {
//            showAlert.value = false
//        },
        confirmButton = {
            TextButton(
                onClick = {
                    onResetConfirm()
                    showAlert.value = false
                }
            ) {
                Text(Resources.BUTTON_CONFIRM)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
//                {
//                    showAlert.value = false
//                }
            ) {
                Text(Resources.BUTTON_DISMISS)
            }
        }
    )
}


