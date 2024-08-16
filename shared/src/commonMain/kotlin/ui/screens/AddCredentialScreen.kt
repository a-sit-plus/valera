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
import androidx.compose.ui.text.input.TextFieldValue
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.heading_label_add_credential_screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.state.savers.CredentialSchemeSaver
import ui.state.savers.asMutableStateSaver
import ui.views.LoadDataView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    var host by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        runBlocking {
            mutableStateOf(TextFieldValue(walletMain.walletConfig.host.first()))
        }
    }

    var credentialRepresentation by rememberSaveable {
        mutableStateOf(runBlocking {
            walletMain.walletConfig.credentialRepresentation.first()
        })
    }

    var credentialScheme by rememberSaveable(
        saver = CredentialSchemeSaver().asMutableStateSaver()
    ) {
        mutableStateOf(runBlocking {
            walletMain.walletConfig.credentialScheme.first()
        })
    }

    var requestedAttributes by rememberSaveable(credentialScheme) {
        runBlocking {
            mutableStateOf(setOf<String>())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScreenHeading(stringResource(Res.string.heading_label_add_credential_screen))
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
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
                walletMain.startProvisioning(
                    host = host.text,
                    credentialScheme = credentialScheme,
                    credentialRepresentation = credentialRepresentation,
                    requestedAttributes = requestedAttributes,
                ) {
                    navigateUp()
                }
            },
            modifier = Modifier.padding(scaffoldPadding)
        )
    }
}