package view

import Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import io.ktor.util.decodeBase64Bytes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen( onAbout: () -> Unit, onCredential: (id: String) -> Unit, onScanQrCode: () -> Unit, walletMain: WalletMain, onLoginWithIdAustria: () -> Unit) {
    Box{
        Column(Modifier.fillMaxSize()) {
            Header(onAbout = onAbout)
            Column(Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (walletMain.subjectCredentialStore.credentialSize.value == 0){
                    AddId(onScanQrCode, onLoginWithIdAustria)
                } else {
                    ShowId(onCredential, onScanQrCode, walletMain, onLoginWithIdAustria)
                }
            }
        }
    }
}

@Composable
fun Header(onAbout: () -> Unit) {
    Row(Modifier.height(80.dp).padding(10.dp).fillMaxWidth().background(color = MaterialTheme.colorScheme.background), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Settings, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { onAbout() }), tint = Color.LightGray.copy(alpha = 0f))
        Text(Resources.DEMO_WALLET, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.Settings, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { onAbout() }), tint = MaterialTheme.colorScheme.onBackground)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AddDialog(openDialog: MutableState<Boolean>, onScanQrCode: () -> Unit, onLoginWithIdAustria: () -> Unit){
    Dialog(onDismissRequest = { openDialog.value = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()){
            Box(Modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))){
                Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = MaterialTheme.colorScheme.tertiaryContainer).fillMaxWidth().padding(20.dp)){
                    Column {
                        Text(Resources.ADD_ID, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(15.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(Modifier.size(15.dp))

                        Text(Resources.ADD_ID_TEXT)
                        Spacer(Modifier.size(30.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onScanQrCode()}).fillMaxWidth()) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                                Image(painterResource(DrawableResource("icons8-qr-code-64.png")), contentDescription = null, Modifier.height(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer))
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(Resources.BUTTON_SCAN_QR, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.size(30.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onLoginWithIdAustria()}).fillMaxWidth()) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                                Image(painterResource(DrawableResource("icons8-login-100.png")), contentDescription = null, Modifier.height(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer))
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(Resources.BUTTON_LOGIN_ID_AUSTRIA, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
fun AddId(onScanQrCode: () -> Unit, onLoginWithIdAustria: () -> Unit) {
    AddIdHeader()
    AddIdBody(onScanQrCode, onLoginWithIdAustria)
}

@Composable
fun AddIdHeader(){
    AddIdText()

}

@Composable
fun AddIdBody(onScanQrCode: () -> Unit, onLoginWithIdAustria: () -> Unit) {
    AddIdCard(onScanQrCode, onLoginWithIdAustria)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AddIdCard(onScanQrCode: () -> Unit, onLoginWithIdAustria: () -> Unit) {
    Box(Modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))){
        Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = MaterialTheme.colorScheme.tertiaryContainer).fillMaxWidth().padding(20.dp)){
            Column {
                Text(Resources.ADD_ID, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(Modifier.size(15.dp))
                Text(Resources.ADD_ID_TEXT)
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onScanQrCode()}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource(DrawableResource("icons8-qr-code-64.png")), contentDescription = null, Modifier.height(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(Resources.BUTTON_SCAN_QR, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {onLoginWithIdAustria()}).fillMaxWidth()) {
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center){
                        Image(painterResource(DrawableResource("icons8-login-100.png")), contentDescription = null, Modifier.height(30.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(Resources.BUTTON_LOGIN_ID_AUSTRIA, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddIdText(){
    Column(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Resources.WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))
        Text(Resources.CHOOSE_NEXT_STEP, fontSize = 30.sp, fontWeight = FontWeight.Bold)

    }
}

@Composable
fun ShowId(onCredential: (id: String) -> Unit, onScanQrCode: () -> Unit, walletMain: WalletMain, onLoginWithIdAustria: () -> Unit) {
    val openDialog = remember { mutableStateOf(false) }
    ShowIdHeader()
    ShowIdCard(onCredential, walletMain)
    Box(Modifier.fillMaxSize().padding(20.dp)){
        Box(Modifier.clip(shape = CircleShape).background(color = MaterialTheme.colorScheme.errorContainer).size(40.dp).clickable(onClick = { openDialog.value = true }).align(Alignment.BottomEnd), contentAlignment = Alignment.Center){
            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
    if (openDialog.value) {
        AddDialog(openDialog, onScanQrCode, onLoginWithIdAustria)
    }

}

@Composable
fun ShowIdHeader(){
    Column(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Resources.WALLET, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp))

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowIdCard(onCredential: (id: String) -> Unit, walletMain: WalletMain) {
    val credentials = walletMain.subjectCredentialStore.getStoreEntries()
    val state = rememberLazyListState()
    LazyRow (flingBehavior = rememberSnapFlingBehavior(lazyListState = state), state = state) {
        items(credentials.size){
            when (val credential = credentials[it]) {
                is SubjectCredentialStore.StoreEntry.Vc -> {
                    IdCard(onCredential, id = credential.vc.jwtId, modifier = Modifier.fillParentMaxWidth(), walletMain)
                }
                is SubjectCredentialStore.StoreEntry.SdJwt -> {
                    IdCard(onCredential, id = credential.sdJwt.jwtId, modifier = Modifier.fillParentMaxWidth(), walletMain)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun IdCard(onCredential: (id: String) -> Unit, id: String, modifier: Modifier, walletMain: WalletMain) {
    val credential = walletMain.subjectCredentialStore.getStoreEntryById(id)
    when(credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> {
            when(val credentialSubject = credential.vc.vc.credentialSubject) {
                is IdAustriaCredential -> {
                    IdAustriaCredentialCard(onCredential, credentialSubject.firstname, credentialSubject.lastname, credentialSubject.portrait, modifier, walletMain, id)
                }
            }
        }
        is SubjectCredentialStore.StoreEntry.SdJwt -> {
            val firstname = credential.disclosures.filter{ it.value?.claimName == "firstname"}.firstNotNullOf { it.value?.claimValue } as String
            val lastname = credential.disclosures.filter{ it.value?.claimName == "lastname"}.firstNotNullOf { it.value?.claimValue } as String
            val portraitEncoded = credential.disclosures.filter{ it.value?.claimName == "portrait"}.firstNotNullOf { it.value?.claimValue } as String
            val portrait = portraitEncoded.decodeBase64Bytes()
            IdAustriaCredentialCard(onCredential, firstname, lastname, portrait, modifier, walletMain, id)
        }
        else -> {}
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun IdAustriaCredentialCard(onCredential: (id: String) -> Unit, firstname: String, lastname: String, portrait: ByteArray?, modifier: Modifier, walletMain: WalletMain, id: String) {
    Box(modifier.padding(start = 20.dp, end = 20.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)).clickable(onClick = {onCredential(id)} )){
        Box(Modifier.clip(shape = RoundedCornerShape(10.dp)).background(color = MaterialTheme.colorScheme.tertiaryContainer).padding(20.dp)){
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Resources.CREDENTIAL, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(Modifier.size(15.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)){
                    if (portrait != null){
                        Image(walletMain.platformAdapter.decodeImage(portrait), contentDescription = "")
                    } else {
                        Image(painterResource(DrawableResource("3d-casual-life-smiling-face-with-smiling-eyes.png")), contentDescription = null, Modifier.size(150.dp), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.size(30.dp))
                Text("$firstname $lastname", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.size(10.dp))
                Text(Resources.ID_AUSTRIA_CREDENTIAL, fontSize = 12.sp)
            }
        }
    }
}

