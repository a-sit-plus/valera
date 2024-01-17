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
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import globalBack
import kotlinx.coroutines.runBlocking


@Composable
fun CredentialScreen(id: String, walletMain: WalletMain){
    val credential = walletMain.subjectCredentialStore.getStoreEntryById(id)
    when(credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> {
            val type = credential.vc.vc.type.filter { it != "VerifiableCredential" }.joinToString(",")
            when(val credentialSubject = credential.vc.vc.credentialSubject) {
                is IdAustriaCredential -> {
                    val firstname = credentialSubject.firstname
                    val lastname = credentialSubject.lastname
                    val dateofbirth = credentialSubject.dateOfBirth.toString()
                    IdAustriaCredentialScreen(firstname, lastname, dateofbirth, id, walletMain, type)
                }
            }
        }
        is SubjectCredentialStore.StoreEntry.SdJwt -> {
            val type = credential.sdJwt.type.filter { it != "VerifiableCredential" }.joinToString(",")
            val firstname = credential.disclosures.filter{ it.value?.claimName == "firstname"}.firstNotNullOf { it.value?.claimValue } as String
            val lastname = credential.disclosures.filter{ it.value?.claimName == "lastname"}.firstNotNullOf { it.value?.claimValue } as String
            val dateofbirth = credential.disclosures.filter{ it.value?.claimName == "date-of-birth"}.firstNotNullOf { it.value?.claimValue } as String
            IdAustriaCredentialScreen(firstname, lastname, dateofbirth, id, walletMain, type)

        }
        else -> {}
    }
}

@Composable
fun IdAustriaCredentialScreen(firstName: String, lastName: String, birthDate: String, id: String, walletMain: WalletMain, type: String) {

    Column {
        Row(Modifier.padding(10.dp).height(80.dp).fillMaxWidth().background(color = MaterialTheme.colorScheme.background), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Close, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { globalBack() }), tint = MaterialTheme.colorScheme.onBackground)
            Text(Resources.DEMO_WALLET, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Info, contentDescription = null, Modifier.size(30.dp).clickable(onClick = { }), tint = Color.LightGray.copy(alpha = 0.0f))
        }
        Column(Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer).fillMaxSize()){
            Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally){
                Text(Resources.ID_AUSTRIA_CREDENTIAL, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(type, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp), horizontalAlignment = Alignment.Start){
                Text(firstName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(Resources.FIRST_NAME, fontSize = 15.sp)
                Spacer(modifier = Modifier.size(10.dp))
                Text(lastName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(Resources.LAST_NAME, fontSize = 15.sp)
                Spacer(modifier = Modifier.size(10.dp))
                Text(birthDate, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(Resources.BIRTH_DATE, fontSize = 15.sp)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally){
                Button(onClick = {
                    runBlocking {
                        walletMain.subjectCredentialStore.removeStoreEntryById(id)
                    }
                    globalBack()
                }) {
                    Text(Resources.BUTTON_DELETE)
                }
            }
        }

    }

}
