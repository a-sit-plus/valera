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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
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
fun HomeScreen( onAbout: () -> Unit) {
    Box(){
        Column(Modifier.fillMaxSize()) {
            Header(onAbout = onAbout)
            Body()
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun Header(onAbout: () -> Unit) {
    Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { onAbout() }), tint = Color.LightGray.copy(alpha = 0f))
        Text("DemoWallet", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.Info, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { onAbout() }), tint = Color.LightGray)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun Body() {
    Column(Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AddId()
    }
}

@Composable
fun AddId(){
    AddIdHeader()
    AddIdBody()
}

@Composable
fun AddIdHeader(){
    AddIdText()

}

@Composable
fun AddIdBody(){
    AddIdCard()
    Box(Modifier.fillMaxSize().padding(20.dp)){
        Box(Modifier.clip(shape = CircleShape).background(color = MaterialTheme.colorScheme.errorContainer).size(40.dp).clickable(onClick = {}).align(Alignment.BottomEnd), contentAlignment = Alignment.Center){
            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AddIdCard() {
    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)){
        Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).fillMaxWidth().padding(20.dp)){
            Column {
                Text("Add ID", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.size(15.dp))
                Divider(color = Color(48, 68, 113), thickness = 1.dp)
                Spacer(Modifier.size(15.dp))
                Text("To add an ID, login on https://abcd.at/xyz/ with a secondary deivce and scan the displayed QR code.")
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource("icons8-qr-code-64.png"), contentDescription = null, Modifier.height(30.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("Scan QR-Code", color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource("icons8-login-100.png"), contentDescription = null, Modifier.height(30.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("Login with ID Austria", color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddIdText(){
    Column(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wallet", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))
        Text("Choose next step", color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.Bold)

    }

}


