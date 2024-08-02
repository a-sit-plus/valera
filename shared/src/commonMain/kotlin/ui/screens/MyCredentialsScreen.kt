package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_add_credential
import composewalletapp.shared.generated.resources.content_description_delete_credential
import composewalletapp.shared.generated.resources.heading_label_my_data_screen
import composewalletapp.shared.generated.resources.info_text_no_credentials_available
import data.storage.scheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.LabeledText
import ui.composables.buttons.LoadDataButton
import ui.composables.inputFields.uiLabel

@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    walletMain: WalletMain,
) {
    MyCredentialsScreen(
        navigateToAddCredentialsPage = navigateToAddCredentialsPage,
        viewModel = CredentialScreenViewModel(walletMain)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    viewModel: CredentialScreenViewModel,
) {
    val storeContainerState by viewModel.storeContainer.collectAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.heading_label_my_data_screen),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        floatingActionButton = {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = navigateToAddCredentialsPage,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.content_description_add_credential),
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
            storeContainerState?.let { storeContainer ->
                if (storeContainer.credentials.isEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = stringResource(Res.string.info_text_no_credentials_available),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LoadDataButton(
                            onClick = navigateToAddCredentialsPage
                        )
                    }
                } else {
                    LazyColumn {
                        items(
                            storeContainer.credentials.size,
                            key = {
                                storeContainer.credentials[it].hashCode()
                            }
                        ) { index ->
                            val credential = storeContainer.credentials[index]

                            SingleCredentialCard(
                                credential,
                                onDelete = {
                                    viewModel.removeCredentialByIndex(index)
                                },
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                ),
                            )
                        }
                        item {
                            FloatingActionButtonHeightSpacer(
                                externalPadding = 16.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SingleCredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SingleCredentialCardLayout(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabeledText(
                text = credential.scheme.uiLabel(),
                label = credential.representation.uiLabel(),
            )
            onDelete?.let {
                SingleCredentialCardDeleteButton(
                    onClick = onDelete
                )
            }
        }
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> SingleVcCredentialCardContent(
                credential = credential,
            )

            is SubjectCredentialStore.StoreEntry.SdJwt -> SingleSdJwtCredentialCardContent(
                credential = credential,
            )

            is SubjectCredentialStore.StoreEntry.Iso -> SingleIsoCredentialCardContent(
                credential = credential,
            )
        }
    }
}

@Composable
private fun ColumnScope.SingleVcCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Vc,
) {
    Text(credential.vc.vc.credentialSubject.toString().replace("""\[.+]""".toRegex(), "[...]").replace(", ", "\n"))
}

@Composable
private fun ColumnScope.SingleSdJwtCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.SdJwt,
) {
    credential.disclosures.forEach {
        LabeledText(
            text = it.value?.claimValue?.toString() ?: "unknown claim value",
            label = it.value?.claimName ?: "unknown claim name"
        )
    }
}

@Composable
private fun ColumnScope.SingleIsoCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Iso,
) {
    credential.issuerSigned.namespaces?.forEach { namespace ->
        namespace.value.entries.forEach { entry ->
            LabeledText(
                text = entry.value.elementValue.prettyToString(),
                label = NormalizedJsonPath(
                    NormalizedJsonPathSegment.NameSegment(namespace.key),
                    NormalizedJsonPathSegment.NameSegment(entry.value.elementIdentifier),
                ).toString(),
            )
        }
    }
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}


@Composable
private fun SingleCredentialCardLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 16.dp
            ).fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun SingleCredentialCardDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.content_description_delete_credential) // TODO: content description
        )
    }
}

private val SubjectCredentialStore.StoreEntry.representation: ConstantIndex.CredentialRepresentation
    get() = when (this) {
        is SubjectCredentialStore.StoreEntry.Vc -> ConstantIndex.CredentialRepresentation.PLAIN_JWT
        is SubjectCredentialStore.StoreEntry.SdJwt -> ConstantIndex.CredentialRepresentation.SD_JWT
        is SubjectCredentialStore.StoreEntry.Iso -> ConstantIndex.CredentialRepresentation.ISO_MDOC
    }