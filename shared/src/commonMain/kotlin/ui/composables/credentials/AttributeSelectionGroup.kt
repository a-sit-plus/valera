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
import at.asitplus.wallet.app.common.getAttributes
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledTextCheckbox

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

            val allAttributes = credential.key.getAttributes()
            val disclosedAttributes = constraints.mapNotNull { constraint ->
                val path = constraint.value.firstOrNull()?.normalizedJsonPath ?: return@mapNotNull null
                val value = allAttributes.toList()
                    .firstOrNull { it.first.segments.last().toString() == path.segments.last().toString() }?.second
                Pair(path, value) to constraint.key.optional
            }.toMap()


            disclosedAttributes.forEach { entry ->
                val path = entry.key.first
                val value = entry.key.second
                val optional = entry.value

                if (selection[path] == null) {
                    selection[path] = if (optional != null) {
                        !optional
                    } else {
                        true
                    }
                }

                val stringResource =
                    format?.getLocalization(NormalizedJsonPath(path.segments.last()))
                if (stringResource != null && optional != null && value != null) {
                    LabeledTextCheckbox(
                        label = stringResource(stringResource),
                        text = value,
                        checked = selection[path] ?: true,
                        onCheckedChange = { bool -> selection[path] = bool },
                        enabled = optional
                    )
                }
            }
        }
    }
}