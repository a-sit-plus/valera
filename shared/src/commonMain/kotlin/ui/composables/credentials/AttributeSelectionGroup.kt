package ui.composables.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledCheckbox

@Composable
fun AttributeSelectionGroup(
    credential: Map.Entry<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>,
    selection: SnapshotStateMap<NormalizedJsonPath, Boolean>,
    format: ConstantIndex.CredentialScheme?
) {
    Column(modifier = Modifier.clip(CardDefaults.shape)) {
        Column(modifier = Modifier
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxWidth()) {
            val constraints = credential.value
            val disclosedAttributes = constraints.values.mapNotNull { constraint ->
                constraint.firstOrNull()?.normalizedJsonPath
            }

            disclosedAttributes.forEach { path ->
                if (selection[path] == null){
                    selection[path] = true
                }
                val stringResource = format?.getLocalization(NormalizedJsonPath(path.segments.last()))
                if (stringResource != null) {
                    LabeledCheckbox(
                        label = stringResource(stringResource),
                        checked = selection[path] ?: true,
                        onCheckedChange = { bool -> selection[path] = bool })
                }
            }
        }
    }
}