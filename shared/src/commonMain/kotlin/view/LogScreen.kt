package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import globalBack
import kotlinx.coroutines.launch

@Composable
fun LogScreen(walletMain: WalletMain){
    val scope = rememberCoroutineScope()

    var logArray = mutableListOf<String>()
    try {
        logArray = walletMain.getLog()
    } catch (e: Throwable) {
        walletMain.errorService.emit(e)
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 80.dp).background(color = MaterialTheme.colorScheme.secondaryContainer), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn {
            items(logArray.size) {
                val color: Color
                if(it % 2 == 0) {
                    color = MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    color = MaterialTheme.colorScheme.secondaryContainer
                }
                Text(text = logArray[it], modifier = Modifier.background(color = color).padding(5.dp).fillMaxWidth(), fontSize = 8.sp, lineHeight = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()){
        Row(modifier = Modifier.height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    globalBack()
                }
            ) {
                Text(Resources.BUTTON_CLOSE)
            }
            Button(
                onClick = {
                    scope.launch {
                        walletMain.platformAdapter.clearFile("log.txt")
                        walletMain.platformAdapter.writeToFile(text = logArray.joinToString(separator = "\n"), "log.txt")
                        walletMain.platformAdapter.shareLog()
                    }
                }
            ) {
                Text("Share")
            }
        }

    }
}

