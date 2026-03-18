package ui.composables.credentials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeListEntry
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import org.koin.compose.koinInject
import ui.models.CredentialFreshnessSummaryModelEvaluator

@Composable
fun CredentialSelectionGroup(
    matchingCredentials: Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, List<NodeListEntry>>>,
    attributeSelection: SnapshotStateMap<String, Boolean>,
    credentialSelection: MutableState<SubjectCredentialStore.StoreEntry>,
    imageDecoder: ImageDecoder = koinInject(),
    checkCredentialFreshness: CredentialFreshnessSummaryModelEvaluator = koinInject(),
) {
    matchingCredentials.forEach { credential ->
        CredentialSelectionCard(
            credential,
            imageDecoder = {
                imageDecoder(it)
            },
            attributeSelection = attributeSelection,
            credentialSelection = credentialSelection,
            checkCredentialFreshness = {
                checkCredentialFreshness(credential.key)
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}