package ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import at.asitplus.jsonpath.core.NormalizedJsonPath
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_add_credential_screen
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.state.savers.CredentialIdentifierInfoSaver
import ui.state.savers.asMutableStateSaver
import ui.views.LoadDataView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadCredentialScreen(
    vm: LoadCredentialViewModel
) {

    var credentialIdentifierInfo by rememberSaveable(CredentialIdentifierInfoSaver().asMutableStateSaver()) {
        mutableStateOf(runBlocking { vm.credentialIdentifiers.first() })
    }

    var requestedAttributes by rememberSaveable(credentialIdentifierInfo) {
        runBlocking {
            mutableStateOf(setOf<NormalizedJsonPath>())
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
        LoadDataView(
            host = vm.hostString,
            credentialIdentifierInfo = credentialIdentifierInfo,
            onChangeCredentialIdentifierInfo = { credentialIdentifierInfo = it },
            requestedAttributes = requestedAttributes,
            onChangeRequestedAttributes = { requestedAttributes = it },
            onSubmit = { vm.onSubmit(credentialIdentifierInfo, requestedAttributes) },
            modifier = Modifier.padding(scaffoldPadding),
            availableIdentifiers = runBlocking { vm.credentialIdentifiers },
        )
    }
}