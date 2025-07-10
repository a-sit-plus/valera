package ui.views.iso.verifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_add_request
import at.asitplus.valera.resources.heading_label_select_combined_data_retrieval_screen
import at.asitplus.valera.resources.info_text_no_requests
import at.asitplus.valera.resources.section_heading_select_document_type
import at.asitplus.valera.resources.text_label_requests
import data.document.SelectableRequest
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.VerifierViewModel
import ui.views.iso.verifier.requests.HIDRequest
import ui.views.iso.verifier.requests.MDLRequests
import ui.views.iso.verifier.requests.PIDRequests

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierCombinedSelectionView(vm: VerifierViewModel) {
    val listSpacingModifier = Modifier.padding(top = 8.dp)
    val layoutSpacingModifier = Modifier.padding(top = 24.dp)

    var showAddButton by remember { mutableStateOf(true) }
    var showRequestTypes by remember { mutableStateOf(false) }
    val selectedRequests = remember { mutableStateListOf<SelectableRequest>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_select_combined_data_retrieval_screen),
                            Modifier.weight(1f),
                        )
                    }
                },
                actions = {
                    Logo(onClick = vm.onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = vm.onClickSettings)) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton({ vm.navigateToVerifyDataView() }) }
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
                    onClick = { vm.onReceiveCombinedSelection(selectedRequests) },
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
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.text_label_requests),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (selectedRequests.isNotEmpty()) {
                        selectedRequests.forEach { requestDocument ->
                            Text(
                                text = requestDocument.type.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.info_text_no_requests),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if(showAddButton) {
                        Button(
                            onClick = {
                                showAddButton = false
                                showRequestTypes = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.button_add_request))
                        }
                    }

                    if (showRequestTypes) {
                        AlertDialog(
                            onDismissRequest = {
                                showRequestTypes = false
                                showAddButton = true
                            },
                            confirmButton = {},
                            title = {
                                Text(stringResource(Res.string.section_heading_select_document_type))
                            },
                            text = {
                                Column {
                                    MDLRequests(layoutSpacingModifier, listSpacingModifier) { request ->
                                        selectedRequests.add(request)
                                        showRequestTypes = false
                                        showAddButton = true
                                    }
                                    PIDRequests(layoutSpacingModifier, listSpacingModifier) { request ->
                                        selectedRequests.add(request)
                                        showRequestTypes = false
                                        showAddButton = true
                                    }
                                    HIDRequest(layoutSpacingModifier, listSpacingModifier) { request ->
                                        selectedRequests.add(request)
                                        showRequestTypes = false
                                        showAddButton = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
