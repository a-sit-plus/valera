package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import composewalletapp.shared.generated.resources.content_description_refresh_credentials
import composewalletapp.shared.generated.resources.heading_label_my_data_screen
import composewalletapp.shared.generated.resources.info_text_no_credentials_available
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.LoadDataButton
import ui.views.MyCredentialsView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MyCredentialsScreen(
    navigateToRefreshCredentialsPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

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
                if(storeContainer.credentials.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = navigateToRefreshCredentialsPage,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.content_description_refresh_credentials),
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
                            onClick = navigateToRefreshCredentialsPage
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(state = rememberScrollState())
                    ) {
                        MyCredentialsView(
                            credentials = storeContainer.credentials,
                            decodeImage = walletMain.platformAdapter::decodeImage,
                        )
                    }
                }
            }
        }
    }
}
