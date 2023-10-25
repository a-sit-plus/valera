package view

import Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.app.common.WalletMain
import data.storage.PersistentSubjectCredentialStore
import kotlinx.coroutines.runBlocking

@Composable
fun PayloadScreen(text: String, onContinueClick: () -> Unit, walletMain: WalletMain){
    Column {
        Text("QR Payload:", fontSize = 25.sp)
        Text(text, fontSize = 18.sp)

        Button(onClick = {
            runBlocking { walletMain.setCredentials()}
            onContinueClick()
        }) {
            Text(Resources.BUTTON_CONTINUE)
        }
    }

}