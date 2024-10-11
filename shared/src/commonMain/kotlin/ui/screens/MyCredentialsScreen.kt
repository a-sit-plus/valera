package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_my_data_screen
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource
import ui.composables.AdvancedFloatingActionButton
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.credentials.CredentialCard
import ui.views.NoDataLoadedView


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
                    NoDataLoadedView(navigateToAddCredentialsPage, navigateToQrAddCredentialsPage)
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
