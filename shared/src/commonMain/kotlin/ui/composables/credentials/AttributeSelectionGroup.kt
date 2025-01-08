package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Card(
        modifier = Modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        ) {
            val constraints = credential.value
            val disclosedAttributes = constraints.values.mapNotNull { constraint ->
                constraint.firstOrNull()?.normalizedJsonPath
            }

            disclosedAttributes.forEach { path ->
                if (selection[path] == null) {
                    selection[path] = true
                }
                val stringResource =
                    format?.getLocalization(NormalizedJsonPath(path.segments.last()))
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