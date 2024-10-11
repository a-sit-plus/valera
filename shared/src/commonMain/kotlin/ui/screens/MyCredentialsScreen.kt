package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_my_data_screen
import compose_wallet_app.shared.generated.resources.info_text_no_credentials_available
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource
import ui.composables.AdvancedFloatingActionButton
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.buttons.CancelButton
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.LoadDataIdaButton
import ui.composables.buttons.LoadDataQrButton
import ui.composables.credentials.CredentialCard

@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    navigateToQrAddCredentialsPage: () -> Unit,
    navigateToCredentialDetailsPage: (Long) -> Unit,
    walletMain: WalletMain,
) {
    MyCredentialsScreen(
        navigateToAddCredentialsPage = navigateToAddCredentialsPage,
        navigateToQrAddCredentialsPage = navigateToQrAddCredentialsPage,
        navigateToCredentialDetailsPage = navigateToCredentialDetailsPage,
        viewModel = CredentialScreenViewModel(walletMain),
        imageDecoder = {
            try {
                walletMain.platformAdapter.decodeImage(it)
            } catch (throwable: Throwable) {
                // TODO: should this be emitted to the error service?
                Napier.w("Failed Operation: decodeImage")
                null
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    navigateToQrAddCredentialsPage: () -> Unit,
    navigateToCredentialDetailsPage: (Long) -> Unit,
    imageDecoder: (ByteArray) -> ImageBitmap?,
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
                    AdvancedFloatingActionButton(addCredential = navigateToAddCredentialsPage, addCredentialQr = navigateToQrAddCredentialsPage)
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
                        val selection = rememberSaveable{mutableStateOf(false)}
                        if (selection.value) {
                            LoadDataIdaButton(navigateToAddCredentialsPage)
                            Spacer(modifier = Modifier.height(5.dp))
                            LoadDataQrButton(navigateToQrAddCredentialsPage)
                            Spacer(modifier = Modifier.height(20.dp))
                            CancelButton(onClick = {selection.value = false})
                        } else {
                            Text(
                                text = stringResource(Res.string.info_text_no_credentials_available),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LoadDataButton(onClick = {selection.value = true})
                        }
                    }
                } else {
                    LazyColumn {
                        items(
                            storeContainer.credentials.size,
                            key = {
                                storeContainer.credentials[it].hashCode()
                            }
                        ) { index ->
                            val storeEntry = storeContainer.credentials[index]
                            val storeEntryIdentifier = storeEntry.first
                            val credential = storeEntry.second

                            Column {
                                CredentialCard(
                                    credential,
                                    onDelete = {
                                        viewModel.removeStoreEntryById(storeEntryIdentifier)
                                    },
                                    onOpenDetails = {
                                        navigateToCredentialDetailsPage(storeEntryIdentifier)
                                    },
                                    imageDecoder = imageDecoder,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    ),
                                )
                            }
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
