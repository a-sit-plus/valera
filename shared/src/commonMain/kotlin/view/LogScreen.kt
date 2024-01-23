package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import globalBack

@Composable
fun LogScreen(walletMain: WalletMain){
    val input = walletMain.platformAdapter.readFromLog() ?: ""
    val log = input.split("\n")


    Column(modifier = Modifier.fillMaxSize().fillMaxSize().padding(top = 10.dp, bottom = 80.dp).background(color = MaterialTheme.colorScheme.secondaryContainer), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn {
            items(log.size) {
                val color: Color
                if(it % 2 == 0) {
                    color = MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    color = MaterialTheme.colorScheme.secondaryContainer
                }
                Text(text = log[it], modifier = Modifier.background(color = color).padding(5.dp).fillMaxWidth(), fontSize = 8.sp, lineHeight = 10.sp)
            }
        }
    }
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()){
        Column(modifier = Modifier.height(80.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Button(
                onClick = {
                    globalBack()
                }
            ) {
                Text(Resources.BUTTON_CLOSE)
            }
        }

    }

}