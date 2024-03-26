package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ui.savers.CredentialSchemeSaver
import ui.savers.asMutableStateSaver
import ui.views.LoadDataView

@Composable
fun LoadDataScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    var host by rememberSaveable(
        saver = object : Saver<MutableState<TextFieldValue>, String> {
            override fun restore(value: String): MutableState<TextFieldValue> {
                return mutableStateOf(TextFieldValue(value))
            }

            override fun SaverScope.save(value: MutableState<TextFieldValue>): String {
                return value.value.text
            }
        }
    ) {
        runBlocking {
            mutableStateOf(TextFieldValue(walletMain.walletConfig.host.first()))
        }
    }

    var credentialRepresentation by rememberSaveable {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.credentialRepresentation.first())
        }
    }

    var credentialScheme by rememberSaveable(
        saver = CredentialSchemeSaver().asMutableStateSaver()
    ) {
        runBlocking {
            mutableStateOf(walletMain.walletConfig.credentialScheme.first())
        }
    }

    var requestedAttributes by rememberSaveable(credentialScheme) {
        mutableStateOf(listOf<String>())
    }

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
        loadData = {
            walletMain.scope.launch {
                try {
                    walletMain.provisioningService.startProvisioning(
                        host = host.text,
                        credentialScheme = credentialScheme,
                        credentialRepresentation = credentialRepresentation,
                        requestedAttributes = requestedAttributes,
                    )
                    navigateUp()
                } catch (e: Exception) {
                    walletMain.errorService.emit(e)
                }
            }
        },
        navigateUp = navigateUp,
    )
}