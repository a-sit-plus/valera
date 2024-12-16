package ui.composables.credentials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSelectionCard(credential: Map.Entry<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>, imageDecoder: (ByteArray) -> ImageBitmap?, attributeSelection: SnapshotStateMap<NormalizedJsonPath, Boolean>, credentialSelection: MutableState<SubjectCredentialStore. StoreEntry>) {
    val selected = remember {mutableStateOf(false)}

    if (credentialSelection.value == credential.key) {
        selected.value = true
    } else {
        selected.value = false
    }

    CredentialSelectionCardLayout(
        onClick = { credentialSelection.value = credential.key },
        modifier = Modifier,
        isSelected = selected
    ) {
        CredentialSelectionCardHeader(
            credential = credential.key,
            modifier = Modifier.fillMaxWidth()
        )
        CredentialSummaryCardContent(
            credential = credential.key,
            decodeToBitmap = imageDecoder,
        )
        CredentialSelectionCardFooter(modifier = Modifier.fillMaxWidth())
    }
    if (credentialSelection.value == credential.key) {
        attributeSelection.clear()
        val format = credential.key.scheme
        AttributeSelectionGroup(credential, format = format, selection = attributeSelection)
    }
}