package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_load_data_screen
import composewalletapp.shared.generated.resources.info_text_redirection_to_id_austria_for_credential_provisioning
import composewalletapp.shared.generated.resources.section_heading_configuration
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.inputFields.IssuingServiceInputField
import ui.composables.inputFields.StatefulCredentialRepresentationInputField
import ui.composables.inputFields.StatefulCredentialSchemeInputField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LoadDataView(
    host: TextFieldValue,
    onChangeHost: (TextFieldValue) -> Unit,
    credentialRepresentation: ConstantIndex.CredentialRepresentation,
    onChangeCredentialRepresentation: (ConstantIndex.CredentialRepresentation) -> Unit,
    credentialScheme: ConstantIndex.CredentialScheme,
    onChangeCredentialScheme: (ConstantIndex.CredentialScheme) -> Unit,
    requestedAttributes: List<String>,
    onChangeRequestedAttributes: (List<String>) -> Unit,
    navigateUp: (() -> Unit)? = null,
    loadData: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.heading_label_load_data_screen),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    LoadDataButton(
                        onClick = loadData
                    )

                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(Res.string.info_text_redirection_to_id_austria_for_credential_provisioning),
                )
                Column {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Text(
                        text = stringResource(Res.string.section_heading_configuration),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    IssuingServiceInputField(
                        value = host,
                        onValueChange = onChangeHost,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    StatefulCredentialRepresentationInputField (
                        value = credentialRepresentation,
                        onValueChange = onChangeCredentialRepresentation,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                    StatefulCredentialSchemeInputField(
                        value = credentialScheme,
                        onValueChange = onChangeCredentialScheme,
                        modifier = listSpacingModifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}