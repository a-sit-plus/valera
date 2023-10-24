package view

import Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import data.idaustria.IdAustriaCredential
import data.storeage.PersistentSubjectCredentialStore
import data.storeage.getVcs
import data.storeage.removeCredentialById
import globalBack
import kotlinx.coroutines.runBlocking


@Composable
fun CredentialScreen(index: Int){
    var credentials = runBlocking { getVcs(PersistentSubjectCredentialStore()) }
    var firstName = ""
    var lastName = ""
    var birthDate = ""

    runBlocking { getVcs(PersistentSubjectCredentialStore()) }

    val credential = credentials[index].credentialSubject
    val vcId = credentials[index].id
    when (credential){
        is IdAustriaCredential ->  {
            firstName = credential.firstname
            lastName = credential.lastname
            birthDate = credential.dateOfBirth.toString()
        }
    }

    Column() {
        Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Close, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { globalBack() }), tint = Color.LightGray)
            Text("DemoWallet", color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Info, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { }), tint = Color.LightGray.copy(alpha = 0.0f))
        }
        Column(Modifier.background(color = MaterialTheme.colorScheme.primaryContainer).fillMaxSize()){
            Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally){
                Text("IDAustria Credential", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("VerifiableCredential, IdAustria2023", fontSize = 15.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp), horizontalAlignment = Alignment.Start){
                Text(firstName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(Resources.FIRST_NAME, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.size(10.dp))
                Text(lastName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(Resources.LAST_NAME, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.size(10.dp))
                Text(birthDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(Resources.BIRTH_DATE, fontSize = 15.sp, color = Color.Black)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally){
                Button(onClick = {
                    runBlocking { removeCredentialById(PersistentSubjectCredentialStore(), vcId) }
                    globalBack()
                }) {
                    Text(Resources.BUTTON_DELETE)
                }
            }
        }

    }


}
