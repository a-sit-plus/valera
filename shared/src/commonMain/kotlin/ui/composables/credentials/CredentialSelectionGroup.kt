package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSelectionGroup(selectedCredential: MutableState<SubjectCredentialStore.StoreEntry>, credentials: Set<SubjectCredentialStore.StoreEntry>, imageDecoder: (ByteArray) -> ImageBitmap?, ) {
    Column {
        credentials.forEach{ credential ->
            CredentialSelectionCard(credential = credential, imageDecoder = imageDecoder, selectedCredential = selectedCredential)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}