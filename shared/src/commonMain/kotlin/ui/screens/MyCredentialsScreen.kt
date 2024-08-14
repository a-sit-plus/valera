package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_add_credential
import composewalletapp.shared.generated.resources.heading_label_my_data_screen
import composewalletapp.shared.generated.resources.info_text_no_credentials_available
import org.jetbrains.compose.resources.stringResource
import ui.composables.FloatingActionButtonHeightSpacer
import ui.composables.buttons.LoadDataButton
import ui.composables.credentials.CredentialCard

@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    walletMain: WalletMain,
) {
    MyCredentialsScreen(
        navigateToAddCredentialsPage = navigateToAddCredentialsPage,
        viewModel = CredentialScreenViewModel(walletMain),
        imageDecoder = walletMain.platformAdapter::decodeImage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCredentialsScreen(
    navigateToAddCredentialsPage: () -> Unit,
    imageDecoder: (ByteArray) -> ImageBitmap,
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
                            val storeEntry = storeContainer.credentials[index]
                            val credential = storeEntry.second

                            CredentialCard(
                                credential,
                                onDelete = {
                                    viewModel.removeCredentialById(storeEntry.first)
                                },
                                imageDecoder = imageDecoder,
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