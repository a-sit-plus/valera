package view

import Resources
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain

@Composable
fun errorScreen(walletMain: WalletMain){
    val throwable = walletMain.errorService.throwable.value
    val message = throwable?.message ?: "Unknown Message"
    val cause = throwable?.cause?.message ?: "Unknown Cause"
    val tint: Color
    val onButton: () -> Unit
    val buttonText: String
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
                Text(message, modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp).fillMaxWidth().verticalScroll(
                    rememberScrollState()
                ), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.size(5.dp))
            Text("Cause:", fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.heightIn(max = 150.dp).background(color = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(cause, modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp).fillMaxWidth().verticalScroll(
                    rememberScrollState()
                ), textAlign = TextAlign.Center)
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