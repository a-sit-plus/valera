package view

import Resources
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.lib.data.dif.CredentialDefinition
import data.PersistentSubjectCredentialStore
import data.getCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import data.testDataStore
import kotlinx.coroutines.runBlocking

var showCredentials = mutableStateOf(false)
var credentialList =  mutableStateOf(mutableListOf<CredentialSubject>())

@Composable
fun HomeScreen( onAbout: () -> Unit, onCredential: (index: Int) -> Unit, onScanQrCode: () -> Unit) {
    Box(){
        Column(Modifier.fillMaxSize()) {
            Header(onAbout = onAbout)
            Column(Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showCredentials.value == false){
                    AddId(onScanQrCode)
                } else {
                    credentialList.value.clear()
                    runBlocking { getCredentials(PersistentSubjectCredentialStore()).forEach { credential ->
                            credentialList.value.add(credential)
                        }
                    }
                    ShowId(onCredential, onScanQrCode)
                }
            }
        }
    }
}

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
fun AddDialog(openDialog: MutableState<Boolean>, onScanQrCode: () -> Unit){
    Dialog(onDismissRequest = { openDialog.value = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()){
            Box(Modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))){
                Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).fillMaxWidth().padding(20.dp)){
                    Column {
                        Text("Add ID", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(Modifier.size(15.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(Modifier.size(15.dp))

                        Text(Resources.ADD_ID_TEXT)
                        Spacer(Modifier.size(30.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onScanQrCode()}).fillMaxWidth()) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                                Image(painterResource("icons8-qr-code-64.png"), contentDescription = null, Modifier.height(30.dp))
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(Resources.BUTTON_SCAN_QR, color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.size(30.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {}).fillMaxWidth()) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                                Image(painterResource("icons8-login-100.png"), contentDescription = null, Modifier.height(30.dp))
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(Resources.BUTTON_LOGIN_ID_AUSTRIA, color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(20.dp)){
                Box(Modifier.clip(shape = CircleShape).background(color = MaterialTheme.colorScheme.errorContainer).size(40.dp).clickable(onClick = { openDialog.value = false }).align(Alignment.BottomEnd), contentAlignment = Alignment.Center){
                    Icon(Icons.Default.Close, contentDescription = null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

    }
}

@Composable
fun AddId(onScanQrCode: () -> Unit) {
    AddIdHeader()
    AddIdBody(onScanQrCode)
}

@Composable
fun AddIdHeader(){
    AddIdText()

}

@Composable
fun AddIdBody(onScanQrCode: () -> Unit) {
    AddIdCard(onScanQrCode)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AddIdCard(onScanQrCode: () -> Unit) {
    Box(Modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))){
        Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).fillMaxWidth().padding(20.dp)){
            Column {
                Text("Add ID", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.size(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(Modifier.size(15.dp))
                Text(Resources.ADD_ID_TEXT)
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onScanQrCode()}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource("icons8-qr-code-64.png"), contentDescription = null, Modifier.height(30.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(Resources.BUTTON_SCAN_QR, color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {CoroutineScope(Dispatchers.Default).launch { testDataStore() }}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource("icons8-login-100.png"), contentDescription = null, Modifier.height(30.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(Resources.BUTTON_LOGIN_ID_AUSTRIA, color = Color(48, 68, 113), fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        Text(Resources.CHOOSE_NEXT_STEP, color = MaterialTheme.colorScheme.primary, fontSize = 30.sp, fontWeight = FontWeight.Bold)

    }
}

@Composable
fun ShowId(onCredential: (index: Int) -> Unit, onScanQrCode: () -> Unit) {
    val openDialog = remember { mutableStateOf(false) }
    ShowIdHeader()
    ShowIdCard(onCredential)
    Box(Modifier.fillMaxSize().padding(20.dp)){
        Box(Modifier.clip(shape = CircleShape).background(color = MaterialTheme.colorScheme.errorContainer).size(40.dp).clickable(onClick = { openDialog.value = true }).align(Alignment.BottomEnd), contentAlignment = Alignment.Center){
            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
    if (openDialog.value == true) {
        AddDialog(openDialog, onScanQrCode)
    }

}

@Composable
fun ShowIdHeader(){
    Column(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wallet", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))

    }
}

@Composable
fun ShowIdCard(onCredential: (index: Int) -> Unit) {
    LazyRow {
        items(credentialList.value.size){
            IdCard(onCredential, index = it, modifier = Modifier.fillParentMaxWidth())
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun IdCard(onCredential: (index: Int) -> Unit, index: Int, modifier: Modifier) {
    var name = ""
    val credential = credentialList.value[index]
    when(credential) {
        is IdAustriaCredential -> {
            name = credential.firstname + " " + credential.lastname
        }
    }


    Box(modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)).clickable(onClick = {onCredential(index)} )){
        Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = Color.White).padding(20.dp)){
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Resources.CREDENTIAL, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.size(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(Modifier.size(15.dp))
                Box(contentAlignment = Alignment.Center){
                    Image(painterResource("3d-casual-life-smiling-face-with-smiling-eyes.png"), contentDescription = null, Modifier.size(150.dp), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.size(30.dp))
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.size(10.dp))
                Text("IDAustria Credential", fontSize = 12.sp, color = Color.Black)

            }
        }
    }
}


