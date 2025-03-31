package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_check_all
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledCheckbox
import ui.composables.LabeledTextCheckbox

@Composable
fun AttributeSelectionGroup(
    credential: Map.Entry<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>,
    selection: SnapshotStateMap<String, Boolean>,
    format: ConstantIndex.CredentialScheme?
) {
    val allChecked = mutableStateOf(false) 
    Card(
        modifier = Modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        ) {
            val constraints = credential.value
            val disclosedAttributes = constraints.mapNotNull { constraint ->
                val path = constraint.value.firstOrNull()?.normalizedJsonPath ?: return@mapNotNull null
                val optional = constraint.key.optional
                val value = constraint.value.first().value.toString()
                Pair(path, value) to optional
            }.toMap()

            LabeledCheckbox(
                label = stringResource(Res.string.text_label_check_all),
                checked = allChecked.value,
                onCheckedChange = { bool ->
                    disclosedAttributes.forEach { entry ->
                        val path = entry.key.first
                        val memberName = (path.segments.last() as NormalizedJsonPathSegment.NameSegment).memberName
                        val optional = entry.value

                        if (optional != null && optional == true) {
                            selection[memberName] = bool
                        }
                        allChecked.value = bool
                    }
                },
                gapWidth = 0.dp
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            disclosedAttributes.forEach { entry ->
                val path = entry.key.first
                val memberName = (path.segments.last() as NormalizedJsonPathSegment.NameSegment).memberName
                val value = entry.key.second
                val optional = entry.value

                if (selection[memberName] == null) {
                    selection[memberName] = if (optional != null) {
                        !optional
                    } else {
                        true
                    }
                }

                val stringResource =
                    format?.getLocalization(NormalizedJsonPath(entry.key.first.segments.last()))
                if (stringResource != null) {
                    LabeledTextCheckbox(
                        label = stringResource(stringResource),
                        text = value,
                        checked = selection[memberName] ?: true,
                        onCheckedChange = { bool -> selection[memberName] = bool },
                        enabled = optional ?: false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}