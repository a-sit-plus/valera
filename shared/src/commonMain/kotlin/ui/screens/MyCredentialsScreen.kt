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
import composewalletapp.shared.generated.resources.CONTENT_DESCRIPTION_REFRESH_CREDENTIALS
import composewalletapp.shared.generated.resources.HEADING_LABEL_MY_DATA_OVERVIEW
import composewalletapp.shared.generated.resources.INFO_TEXT_NO_CREDENTIALS_AVAILABLE
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
                        stringResource(Res.string.HEADING_LABEL_MY_DATA_OVERVIEW),
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
                            contentDescription = stringResource(Res.string.CONTENT_DESCRIPTION_REFRESH_CREDENTIALS),
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
                            text = stringResource(Res.string.INFO_TEXT_NO_CREDENTIALS_AVAILABLE),
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
