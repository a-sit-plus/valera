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
import ui.state.savers.CredentialSchemeSaver
import ui.state.savers.asMutableStateSaver
import ui.views.LoadDataView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    vm: AddCredentialViewModel
) {
    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        runBlocking {
            mutableStateOf(TextFieldValue(vm.hostString))
        }
    }

    var credentialScheme by rememberSaveable(
        saver = CredentialSchemeSaver().asMutableStateSaver()
    ) {
        mutableStateOf(runBlocking {
            vm.availableSchemes.first()
        })
    }

    var credentialRepresentation by rememberSaveable {
        mutableStateOf(runBlocking {
            vm.walletMain.walletConfig.credentialRepresentation.first()
        })
    }

    LaunchedEffect(credentialScheme) {
        if(credentialRepresentation !in credentialScheme.supportedRepresentations) {
            credentialRepresentation = credentialScheme.supportedRepresentations.first()
        }
    }

    var requestedAttributes by rememberSaveable(credentialScheme) {
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
            host = host,
            onChangeHost = {
                host = it
            },
            credentialRepresentation = credentialRepresentation,
            onChangeCredentialRepresentation = {
                credentialRepresentation = it
            },
            credentialScheme = credentialScheme,
            onChangeCredentialScheme = {
                credentialScheme = it
            },
            requestedAttributes = requestedAttributes,
            onChangeRequestedAttributes = {
                requestedAttributes = it
            },
            onSubmit = {
                vm.onSubmit(host.text, credentialScheme, credentialRepresentation, requestedAttributes)
            },
            modifier = Modifier.padding(scaffoldPadding),
            availableSchemes = vm.availableSchemes,
            showAttributes = vm.showAttributes,
        )
    }
}