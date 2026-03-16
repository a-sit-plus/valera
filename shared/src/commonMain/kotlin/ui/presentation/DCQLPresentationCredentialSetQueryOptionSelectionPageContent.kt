package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.data.NonEmptyList
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.section_heading_requested_data
import at.asitplus.valera.resources.text_label_mandatory_dataset
import at.asitplus.valera.resources.text_label_mandatory_dataset_description
import at.asitplus.valera.resources.text_label_optional_dataset
import at.asitplus.valera.resources.text_label_optional_dataset_description
import at.asitplus.valera.resources.text_label_select_no_optional_credential_set_query_option
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading

@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
    isCredentialSetQueryRequired: Boolean,
    selectedOptionIndex: UInt?,
    credentialSetQueryOptionUiModels: NonEmptyList<CredentialSetQueryOptionUiModel>,
    onSelectCredentialSetQueryOptionAtIndex: (UInt) -> Unit,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
) {
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = null,
                onAbort = onAbort,
                onContinue = onContinue,
                useBackButton = true,
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(it).fillMaxSize().verticalScroll(state = rememberScrollState()).padding(16.dp),
        ) {
            ScreenHeading(stringResource(Res.string.section_heading_requested_data))

            val (datasetLabel, datasetLabelDescription) = if (isCredentialSetQueryRequired) {
                Res.string.text_label_mandatory_dataset to Res.string.text_label_mandatory_dataset_description
            } else {
                Res.string.text_label_optional_dataset to Res.string.text_label_optional_dataset_description
            }

            Text(
                // truncated due to selection semantics - only showing unsatisfied
                stringResource(datasetLabel), //  + " #$currentCredentialSetQueryIndexPlus1/$totalCredentialSetQueries",
                fontWeight = FontWeight.Companion.Bold
            )
            Text(stringResource(datasetLabelDescription))

            if (!isCredentialSetQueryRequired) {
                ElevatedCard(
                    onClick = { onSelectCredentialSetQueryOptionAtIndex(0u) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                    ) {
                        BoldCredentialSchemeText(stringResource(Res.string.text_label_select_no_optional_credential_set_query_option))
                        RadioButton(
                            selected = selectedOptionIndex == 0u,
                            onClick = { onSelectCredentialSetQueryOptionAtIndex(0u) },
                        )
                    }
                }
            }

            for ((index, credentialSetQueryOption) in credentialSetQueryOptionUiModels.withIndex().sortedBy {
                !it.value.isSatisfiable
            }) {
                val virtualIndexWithOptional = index.toUInt() + if(isCredentialSetQueryRequired) {
                    0u
                } else {
                    1u
                }
                DCQLCredentialSetQueryOptionSelectionCard(
                    isSatisfiable = credentialSetQueryOption.isSatisfiable,
                    isSelected = virtualIndexWithOptional == selectedOptionIndex,
                    credentialQueryUiModels = credentialSetQueryOption.credentialQueries,
                ) {
                    onSelectCredentialSetQueryOptionAtIndex(virtualIndexWithOptional)
                }
            }
        }
    }
}

