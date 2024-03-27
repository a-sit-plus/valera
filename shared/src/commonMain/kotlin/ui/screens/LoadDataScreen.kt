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
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.dif.FormatHolder
import at.asitplus.wallet.lib.data.jsonSerializer
import data.CredentialExtractor
import data.storage.scheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ui.navigation.RefreshRequirements
import ui.savers.CredentialSchemeSaver
import ui.savers.asMutableStateSaver
import ui.views.StatefulLoadDataView

@Composable
fun LoadDataScreen(
    refreshRequirements: RefreshRequirements?,
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
        val requestedFormatHolder = refreshRequirements?.requestedCredentialFormatHolderStringified?.let { jsonSerializer.decodeFromString<FormatHolder>(it) }

        mutableStateOf(if (requestedFormatHolder == null) {
            runBlocking {
                walletMain.walletConfig.credentialRepresentation.first()
            }
        } else {
            if (requestedFormatHolder.msoMdoc != null) {
                ConstantIndex.CredentialRepresentation.ISO_MDOC
            } else if (requestedFormatHolder.jwtVp != null) {
                ConstantIndex.CredentialRepresentation.PLAIN_JWT
            } else if (requestedFormatHolder.jwtSd != null) {
                at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
            } else {
                throw Exception("No supported format found: $requestedFormatHolder")
            }
        })
    }

    var credentialScheme by rememberSaveable(
        saver = CredentialSchemeSaver().asMutableStateSaver()
    ) {
        refreshRequirements?.requestedCredentialSchemeIdentifier?.let {
            AttributeIndex.resolveAttributeType(it) ?: AttributeIndex.resolveIsoNamespace(it) ?: AttributeIndex.resolveSchemaUri(it)
        }?.let { mutableStateOf(it) } ?: runBlocking {
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
            val requestedAttributes = credentialScheme.claimNames.filter {
                credentialExtractor.containsAttribute(it)
            }.toSet() + (refreshRequirements?.requestedAttributes ?: setOf())
            mutableStateOf(requestedAttributes)
        }
    }

    val isEditEnabled = refreshRequirements == null

    StatefulLoadDataView(
        isEditEnabled = isEditEnabled,
        host = host,
        onChangeHost = {
            if (isEditEnabled) {
                host = it
            }
        },
        credentialRepresentation = credentialRepresentation,
        onChangeCredentialRepresentation = {
            if (isEditEnabled) {
                credentialRepresentation = it
            }
        },
        credentialScheme = credentialScheme,
        onChangeCredentialScheme = {
            if (isEditEnabled) {
                credentialScheme = it
            }
        },
        availableCredentials = availableCredentials,
        requestedAttributes = requestedAttributes,
        onChangeRequestedAttributes = {
            if (isEditEnabled) {
                requestedAttributes = it
            }
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