package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_custom_data_retrieval_screen
import at.asitplus.valera.resources.section_heading_select_document_type
import at.asitplus.valera.resources.section_heading_select_requested_data_entries
import at.asitplus.valera.resources.section_heading_selected_namespace
import data.document.DocumentAttributes
import data.document.itemsToRequestDocument
import data.document.DocType.documentTypeToNameSpace
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierCustomSelectionView(vm: VerifierViewModel) {
    val vm = remember { vm }
    var selectedDocumentType by remember { mutableStateOf(documentTypeToNameSpace.keys.toList()[0]) }
    var selectedEntries by remember { mutableStateOf(setOf<StringResource>()) }

    // TODO:
    //  - fix possible data selection (default per type un-/select handling)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_select_custom_data_retrieval_screen),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo(onClick = vm.onClickLogo)
                    }
                },
                navigationIcon = { NavigateUpButton({ vm.navigateToVerifyDataView() }) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward),
                            contentDescription = "",
                        )
                    },
                    label = {},
                    onClick = {
                        vm.onReceiveCustomSelection(
                            itemsToRequestDocument(
                                selectedDocumentType,
                                documentTypeToNameSpace[selectedDocumentType]!!,
                                selectedEntries
                            )
                        )
                    },
                    selected = false,
                )
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column(modifier = layoutSpacingModifier) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_select_document_type),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    for (item in documentTypeToNameSpace.keys.toList()) {
                        singleChoiceButton(
                            item,
                            selectedDocumentType,
                            listSpacingModifier
                        ) {
                            selectedDocumentType = item
                        }
                    }
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_selected_namespace) + documentTypeToNameSpace[selectedDocumentType],
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(modifier = layoutSpacingModifier) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_select_requested_data_entries),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    for (item in DocumentAttributes.entries.toList()) {
                        val current = item.displayName
                        multipleChoiceButton(
                            current,
                            selectedEntries.contains(current),
                            selectedEntries.contains(current),
                            listSpacingModifier
                        ) {
                            selectedEntries = if (selectedEntries.contains(current)) {
                                selectedEntries - current
                            } else {
                                selectedEntries + current
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun singleChoiceButton(
    current: String,
    selectedOption: String,
    modifier: Modifier = Modifier,
    onOptionSelected: () -> Unit,
) {
    Row(
        modifier = modifier.selectable(
            selected = (current == selectedOption),
            onClick = onOptionSelected,
            role = Role.RadioButton
        )
    ) {
        RadioButton(
            selected = (current == selectedOption),
            onClick = null
        )
        Text(text = (current))
    }
}

@Composable
private fun multipleChoiceButton(
    current: StringResource,
    value: Boolean,
    contains: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier.toggleable(
            value = value,
            onValueChange = onValueChange,
            role = Role.Checkbox
        )
    ) {
        Checkbox(
            checked = contains,
            onCheckedChange = null
        )
        Text(text = stringResource(current))
    }
}
