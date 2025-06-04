package ui.views.iso.verifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_custom_data_retrieval_screen
import at.asitplus.valera.resources.section_heading_select_document_type
import at.asitplus.valera.resources.section_heading_select_requested_data_entries
import at.asitplus.valera.resources.section_heading_selected_namespace
import data.document.RequestDocumentBuilder
import data.document.SelectableDocTypes
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierCustomSelectionView(
    vm: VerifierViewModel,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUp: () -> Unit,
) {
    val listSpacingModifier = Modifier.padding(top = 8.dp)
    val layoutSpacingModifier = Modifier.padding(top = 24.dp)

    var selectedDocumentType by remember { mutableStateOf(SelectableDocTypes.docTypes.first()) }
    var selectedEntries by remember {
        mutableStateOf(RequestDocumentBuilder.getPreselection(SelectableDocTypes.docTypes.first()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_select_custom_data_retrieval_screen),
                            Modifier.weight(1f),
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        RequestDocumentBuilder.getDocTypeConfig(selectedDocumentType)?.let { config ->
                            val items = RequestDocumentBuilder.buildRequestDocument(
                                scheme = config.scheme,
                                subSet = selectedEntries
                            )
                            vm.onReceiveCustomSelection(items, vm.selectedEngagementMethod.value)
                        }
                    },
                    selected = false
                )
            }
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Document selection
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_select_document_type),
                        style = MaterialTheme.typography.titleMedium
                    )
                    SelectableDocTypes.docTypes.forEach { docType ->
                        singleChoiceButton(docType, selectedDocumentType, listSpacingModifier) {
                            selectedDocumentType = docType
                            selectedEntries = RequestDocumentBuilder.getPreselection(docType)
                        }
                    }
                }
                val docTypeConfig = RequestDocumentBuilder.getDocTypeConfig(selectedDocumentType)
                // Namespace info
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(
                            Res.string.section_heading_selected_namespace,
                            docTypeConfig?.scheme?.isoNamespace!!
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Attribute selection
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_select_requested_data_entries),
                        style = MaterialTheme.typography.titleMedium
                    )
                    docTypeConfig?.let { config ->
                        config.scheme.claimNames.forEach { element ->
                            multipleChoiceButton(
                                config.translator(
                                    NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment(element))
                                )?.let { stringResource(it) } ?: element,
                                selectedEntries.contains(element),
                                selectedEntries.contains(element),
                                listSpacingModifier
                            ) {
                                selectedEntries = if (selectedEntries.contains(element)) {
                                    selectedEntries - element
                                } else {
                                    selectedEntries + element
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun singleChoiceButton(
    current: String,
    selectedOption: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onOptionSelected: () -> Unit,
) {
    Row(
        modifier = modifier.selectable(
            selected = (current == selectedOption),
            onClick = onOptionSelected,
            role = Role.RadioButton
        )
    ) {
        RadioButton(selected = (current == selectedOption), onClick = null)
        icon?.invoke()
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = current)
    }
}

@Composable
private fun multipleChoiceButton(
    current: String,
    value: Boolean,
    contains: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.toggleable(
            value = value,
            onValueChange = onValueChange,
            role = Role.Checkbox
        )
    ) {
        Checkbox(checked = contains, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = current)
    }
}
