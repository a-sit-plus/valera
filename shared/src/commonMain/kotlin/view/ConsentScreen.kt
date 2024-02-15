package view

import Resources
import Resources.ID_AUSTRIA_CREDENTIAL
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appLink
import at.asitplus.wallet.app.common.WalletMain
import globalBack
import kotlinx.coroutines.launch

@Composable
fun ConsentScreen(walletMain: WalletMain, onAccept: () -> Unit, onCancel: () -> Unit, recipientName: String, recipientLocation: String, claims: List<String>){
    Column {
        Row(Modifier.height(80.dp).padding(10.dp).fillMaxWidth().background(color = MaterialTheme.colorScheme.background), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, Modifier.size(30.dp).clickable(onClick = {  }), tint = Color.LightGray.copy(alpha = 0f))
            Text(Resources.LOGIN, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Close, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { globalBack() }), tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(30.dp))

            Text(Resources.LOGIN_TERMINAL_MACHINE, fontSize = 20.sp)
            Spacer(modifier = Modifier.size(20.dp))
            Column(Modifier.height(60.dp).width(120.dp).background(color = MaterialTheme.colorScheme.tertiaryContainer), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("SP IMAGE")
            }
            Spacer(modifier = Modifier.size(40.dp))
            Column(Modifier.fillMaxWidth(). padding(horizontal = 20.dp), horizontalAlignment = Alignment.Start) {
                Text(Resources.RECIPIENT, fontSize = 25.sp)
                Spacer(modifier = Modifier.size(10.dp))
                Text(Resources.NAME, fontSize = 15.sp)
                Text(Resources.LOCATION, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.size(20.dp))
            Column(Modifier.fillMaxWidth(). padding(horizontal = 20.dp), horizontalAlignment = Alignment.Start) {
                Text(Resources.REQUESTED_DATA, fontSize = 25.sp)
                Spacer(modifier = Modifier.size(10.dp))
                if (claims.size > 0) {
                    claims.forEach {
                        Text(it, fontSize = 15.sp)
                    }
                } else {
                    Text(ID_AUSTRIA_CREDENTIAL, fontSize = 15.sp)
                }

            }
        }
    }
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.height(130.dp).fillMaxWidth().background(color = MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(20.dp))
            Text(Resources.TRANSMIT_THIS_INFORMATION, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(10.dp))
            Row(modifier = Modifier.height(70.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        appLink.value = null
                        onCancel()
                    }
                ) {
                    Text(Resources.BUTTON_CANCEL)
                }
                Button(
                    onClick = {
                        walletMain.scope.launch {
                            try {
                                walletMain.presentationService.startSiop(appLink.value.toString())
                            } catch (e: Throwable) {
                                walletMain.errorService.emit(e)
                            }
                            appLink.value = null
                            onAccept()
                        }
                    }
                ) {
                    Text(Resources.BUTTON_ACCEPT)
                }
            }
        }
    }
}