package ui.views.iso.verifier

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.openid.OpenId4VciClaimsPathPointer
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_select_custom_data_retrieval_screen
import at.asitplus.valera.resources.section_heading_select_document_type
import at.asitplus.valera.resources.section_heading_select_requested_data_entries
import at.asitplus.valera.resources.section_heading_selected_namespace
import data.credentials.SelectableCredentialSchemes
import data.credentials.mandatoryClaimPaths
import data.credentials.metadataLabel
import data.credentials.toIsoElementIdentifier
import data.credentials.toNormalizedJsonPathOrNull
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.iso.common.MultipleChoiceButton
import ui.views.iso.common.SingleChoiceButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierCustomSelectionView(
    onClickLogo: () -> Unit,
    navigateUp: () -> Unit,
    vm: VerifierViewModel
) {
    val listSpacingModifier = Modifier.padding(top = 8.dp)
    val layoutSpacingModifier = Modifier.padding(top = 24.dp)
    val selectableSchemes = remember { SelectableCredentialSchemes.schemes }

    var selectedScheme by remember { mutableStateOf(selectableSchemes.first()) }
    var selectedEntries by remember {
        mutableStateOf<Set<OpenId4VciClaimsPathPointer>>(
            selectedScheme.mandatoryClaimPaths().toSet()
        )
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
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton(navigateUp) }
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
                    onClick = { vm.onReceiveCustomSelection(selectedScheme, selectedEntries) },
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
                    selectableSchemes.forEach { scheme ->
                        val docType = scheme.isoDocType ?: return@forEach
                        SingleChoiceButton(docType, selectedScheme.isoDocType.orEmpty(), listSpacingModifier) {
                            selectedScheme = scheme
                            selectedEntries = scheme.mandatoryClaimPaths().toSet()
                        }
                    }
                }
                // Namespace info
                Column(modifier = layoutSpacingModifier) {
                    Text(
                        text = stringResource(
                            Res.string.section_heading_selected_namespace,
                            selectedScheme.isoNamespace!!
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
                    val namespace = selectedScheme.isoNamespace!!
                    val claimEntries: List<Pair<OpenId4VciClaimsPathPointer, String>> = selectedScheme.claimDescriptions
                        .map { it.path }
                        .mapNotNull { claim: OpenId4VciClaimsPathPointer ->
                            claim.toIsoElementIdentifier(namespace)?.let { element -> claim to element }
                        }
                    claimEntries.forEach { (claim, element) ->
                        MultipleChoiceButton(
                            claim.toNormalizedJsonPathOrNull()?.let { path -> selectedScheme.metadataLabel(path) }
                                ?: selectedScheme.metadataLabel(
                                    NormalizedJsonPath(NormalizedJsonPathSegment.NameSegment(element))
                                )
                                ?: element,
                            selectedEntries.contains(claim),
                            selectedEntries.contains(claim),
                            listSpacingModifier
                        ) {
                            selectedEntries = selectedEntries.toMutableSet().also { entries ->
                                if (!entries.remove(claim)) entries.add(claim)
                            }
                        }
                    }
                }
            }
        }
    }
}
