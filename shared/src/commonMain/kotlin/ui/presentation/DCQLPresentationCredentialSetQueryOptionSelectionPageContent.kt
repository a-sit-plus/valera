package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.data.NonEmptyList
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.section_heading_requested_data
import at.asitplus.valera.resources.text_label_mandatory_dataset
import at.asitplus.valera.resources.text_label_mandatory_dataset_description
import at.asitplus.valera.resources.text_label_optional_dataset
import at.asitplus.valera.resources.text_label_optional_dataset_description
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

            for ((index, credentialSetQueryOption) in credentialSetQueryOptionUiModels.withIndex().sortedBy {
                !it.value.isSatisfiable
            }) {
                DCQLCredentialSetQueryOptionSelectionCard(
                    isSatisfiable = credentialSetQueryOption.isSatisfiable,
                    isSelected = index.toUInt() == selectedOptionIndex,
                    credentialQueryUiModels = credentialSetQueryOption.credentialQueries,
                ) {
                    onSelectCredentialSetQueryOptionAtIndex(index.toUInt())
                }
            }
        }
    }
}

