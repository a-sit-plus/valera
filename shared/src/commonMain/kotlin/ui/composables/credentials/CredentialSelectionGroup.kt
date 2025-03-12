package ui.composables.credentials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

@Composable
fun CredentialSelectionGroup(
    matchingCredentials: Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>,
    attributeSelection: SnapshotStateMap<String, Boolean>,
    credentialSelection: MutableState<SubjectCredentialStore.StoreEntry>,
    imageDecoder: (ByteArray) -> ImageBitmap?
) {
    matchingCredentials.forEach { credential ->
        CredentialSelectionCard(
            credential,
            imageDecoder = imageDecoder,
            attributeSelection = attributeSelection,
            credentialSelection = credentialSelection
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}