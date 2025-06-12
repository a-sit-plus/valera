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
import at.asitplus.wallet.lib.agent.validation.CredentialFreshnessSummary

@Composable
fun CredentialSelectionGroup(
    matchingCredentials: Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>,
    attributeSelection: SnapshotStateMap<String, Boolean>,
    credentialSelection: MutableState<SubjectCredentialStore.StoreEntry>,
    imageDecoder: (ByteArray) -> ImageBitmap?,
    checkFreshness: suspend (SubjectCredentialStore.StoreEntry) -> CredentialFreshnessSummary?,
) {
    matchingCredentials.forEach { credential ->
        CredentialSelectionCard(
            credential,
            imageDecoder = imageDecoder,
            attributeSelection = attributeSelection,
            credentialSelection = credentialSelection,
            checkFreshness = { checkFreshness(credential.key) },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}