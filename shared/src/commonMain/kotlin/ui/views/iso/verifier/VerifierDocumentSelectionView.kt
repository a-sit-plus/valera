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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_check_combined
import at.asitplus.valera.resources.button_label_check_custom
import at.asitplus.valera.resources.heading_label_select_data_retrieval_screen
import at.asitplus.valera.resources.section_heading_request_combined
import at.asitplus.valera.resources.section_heading_request_custom
import data.document.SelectableRequest
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.iso.verifier.requests.HIIDRequest
import ui.views.iso.verifier.requests.MDLRequests
import ui.views.iso.verifier.requests.PIDRequests
import ui.views.iso.verifier.requests.RequestItem
import ui.views.iso.verifier.requests.RequestItemData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierDocumentSelectionView(
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    vm: VerifierViewModel,
    bottomBar: @Composable () -> Unit
) {
    val listSpacingModifier = Modifier.padding(top = 8.dp).fillMaxWidth()
    val layoutSpacingModifier = Modifier.padding(top = 24.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_select_data_retrieval_screen),
                            Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton({ vm.onResume() }) }
            )
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val handleRequest: (SelectableRequest) -> Unit = { request ->
                    vm.onRequestSelected(request)
                }
                Column {
                    MDLRequests(Modifier, listSpacingModifier, handleRequest)
                    PIDRequests(layoutSpacingModifier, listSpacingModifier, handleRequest)
                    HIIDRequest(layoutSpacingModifier, listSpacingModifier, handleRequest)
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_combined),
                        style = MaterialTheme.typography.titleMedium
                    )
                    RequestItem(
                        RequestItemData(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.SwitchAccount,
                                    contentDescription = null
                                )
                            },
                            label = stringResource(Res.string.button_label_check_combined),
                            onClick = { vm.navigateToCombinedSelectionView() }
                        ),
                        modifier = listSpacingModifier
                    )
                }
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_custom),
                        style = MaterialTheme.typography.titleMedium
                    )
                    RequestItem(
                        RequestItemData(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Build,
                                    contentDescription = null
                                )
                            },
                            label = stringResource(Res.string.button_label_check_custom),
                            onClick = { vm.navigateToCustomSelectionView() }
                        ),
                        modifier = listSpacingModifier
                    )
                }
            }
        }
    }
}

