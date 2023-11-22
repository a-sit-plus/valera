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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import globalBack

@Composable
fun AppLinkScreen(url: String){
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Resources.COMPOSE_WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(Resources.REDIRECT_TITLE, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Box(Modifier.fillMaxWidth().padding(20.dp)){
            Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).fillMaxWidth().padding(20.dp)){
                Column {
                    Text(Resources.URL + " : " + url)
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
    }
}