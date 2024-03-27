package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import at.asitplus.wallet.app.common.WalletMain
import data.CredentialExtractor
import data.storage.scheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ui.savers.CredentialSchemeSaver
import ui.savers.asMutableStateSaver
import ui.views.StatefulLoadDataView

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

    val availableCredentials by walletMain.subjectCredentialStore.observeStoreContainer().map {
        it.credentials
    }.collectAsState(listOf())

    var requestedAttributes by rememberSaveable(credentialScheme) {
        runBlocking {
            val storeContainer = walletMain.subjectCredentialStore.observeStoreContainer().first()
            val credentialExtractor =
                CredentialExtractor(storeContainer.credentials.filter { it.scheme == credentialScheme })
            mutableStateOf(credentialScheme.claimNames.filter {
                credentialExtractor.containsAttribute(it)
            }.toSet())
        }
    }

    StatefulLoadDataView(
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
        availableCredentials = availableCredentials,
        requestedAttributes = requestedAttributes,
        onChangeRequestedAttributes = {
            requestedAttributes = it
        },
        refreshData = {
            walletMain.scope.launch {
                if (requestedAttributes.isEmpty()) {
                    walletMain.subjectCredentialStore.reset()
                    navigateUp()
                } else {
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
            }
        },
        navigateUp = navigateUp,
    )
}