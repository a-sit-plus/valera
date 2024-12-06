package ui.views.Authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.getLocalization
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.button_label_continue
import compose_wallet_app.shared.generated.resources.heading_label_navigate_back
import compose_wallet_app.shared.generated.resources.prompt_select_attribute
import data.RequestOptionParameters
import org.jetbrains.compose.resources.stringResource
import ui.composables.LabeledCheckbox
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.Authentication.AuthenticationAttributesSelectionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationAttributesSelectionView(vm: AuthenticationAttributesSelectionViewModel){
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_navigate_back),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    NavigateUpButton({ vm.navigateUp() })
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.prompt_select_attribute),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(onClick = {
                            val selection = vm.selectedAttributes.entries.associate { it.key to it.value.filter { it.key.value }.values.toSet() }
                            vm.selectAttributes(selection)
                        }){
                            Text(stringResource(Res.string.button_label_continue))
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()).padding(16.dp),
            ) {
                vm.requests.forEach { request ->
                    val params = request.value.first
                    val credential = vm.selectedCredentials[request.key]
                    val constraints = request.value.second[credential]
                    val disclosedAttributes = constraints?.values?.mapNotNull { constraint ->
                        constraint.firstOrNull()?.normalizedJsonPath
                    }
                    val selections: MutableMap<MutableState<Boolean>, NormalizedJsonPath> = mutableMapOf()
                    disclosedAttributes?.forEach { attribute ->
                        selections[mutableStateOf(true)] = attribute
                    }
                    vm.selectedAttributes[request.key] = selections
                    AttributeSelectionGroup(params = params, selections = selections)
                }
            }
        }
    }
}

@Composable
fun AttributeSelectionGroup(params: RequestOptionParameters, selections:  MutableMap<MutableState<Boolean>, NormalizedJsonPath>){
    Column {
        Text(text = params.credentialIdentifier,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,)
        params.attributes?.forEach { attribute ->
            val format = params.resolved?.first
            val label = format?.getLocalization(NormalizedJsonPath() + attribute)
            val selection = selections.filter { it.value.toString().contains(attribute) }.firstNotNullOf { it }
            if (label != null){
                Row {
                    LabeledCheckbox(label = stringResource(label), checked = selection.key.value, onCheckedChange = {bool -> selection.key.value = bool})
                }
            }
        }
    }
}
