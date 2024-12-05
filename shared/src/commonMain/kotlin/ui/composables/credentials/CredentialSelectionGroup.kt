package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.RequestOptionParameters

@Composable
fun CredentialSelectionGroup(selectedCredential: MutableState<SubjectCredentialStore.StoreEntry>, request:  Map. Entry<String, Pair<RequestOptionParameters, Map<SubjectCredentialStore. StoreEntry, Map<ConstraintField, NodeList>>>>, imageDecoder: (ByteArray) -> ImageBitmap?, ) {
    Column {
        request.value.second.forEach{ credential ->
            CredentialSelectionCard(credential = credential.key, imageDecoder = imageDecoder, selectedCredential = selectedCredential)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}