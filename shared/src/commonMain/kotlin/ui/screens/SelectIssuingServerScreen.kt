package ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import at.asitplus.jsonpath.core.NormalizedJsonPath
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_add_credential_screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.composables.forms.SelectIssuingServerForm
import ui.state.savers.CredentialSchemeSaver
import ui.state.savers.asMutableStateSaver
import ui.views.LoadDataView
import ui.views.SelectIssuingServerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectIssuingServerScreen(
    vm: AddCredentialViewModel
) {
    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        runBlocking {
            mutableStateOf(TextFieldValue(vm.hostString))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenHeading(stringResource(Res.string.heading_label_add_credential_screen))
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
        },
    ) { scaffoldPadding ->
        SelectIssuingServerView(
            host = host,
            onChangeHost = { host = it },
            onSubmit = { vm.onSubmitServer(host.text) },
            modifier = Modifier.padding(scaffoldPadding),
        )
    }
}