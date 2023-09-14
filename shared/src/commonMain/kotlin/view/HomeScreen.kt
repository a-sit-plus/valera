package view

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen() {
    Box(){
        Column(Modifier.fillMaxSize()) {
            Header()
            Body()
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun Header() {
    Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("")
        Text("Wallet", color = MaterialTheme.colorScheme.primary, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.Info, contentDescription = null, Modifier.size(30.dp).clickable(onClick = {  }), tint = Color.LightGray)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun Body() {
    Column(Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    }
}
