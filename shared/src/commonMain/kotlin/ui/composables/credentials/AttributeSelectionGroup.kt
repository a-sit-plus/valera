package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
    Column {
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