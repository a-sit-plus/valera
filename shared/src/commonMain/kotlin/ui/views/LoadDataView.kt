package ui.views

import Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.launch
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton

@Composable
fun LoadDataScreen(
    navigateUp: () -> Unit,
    navigateToQrCodeCredentialProvisioningPage: () -> Unit,
    walletMain: WalletMain,
) {
    LoadDataView(
        loadData = {
            walletMain.scope.launch {
                try {
                    walletMain.provisioningService.startProvisioning()
                    navigateUp()
                } catch (e: Exception) {
                    walletMain.errorService.emit(e)
                }
            }
        },
        navigateUp = navigateUp,
        navigateToQrCodeCredentialProvisioningPage = navigateToQrCodeCredentialProvisioningPage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDataView(
    loadData: () -> Unit,
    navigateUp: (() -> Unit)? = null,
    navigateToQrCodeCredentialProvisioningPage: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Resources.HEADING_LABEL_LOAD_DATA, // "Daten Laden",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
//                    OutlinedTextIconButton(
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Default.QrCode,
//                                contentDescription = null,
//                            )
//                        },
//                        text = {
//                            Text(
//                                "Scan QR-Code",
//                            )
//                        },
//                        onClick = navigateToQrCodeCredentialProvisioningPage,
//                    )
                    LoadDataButton(loadData)
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text(
                "Zur Abfrage Ihrer Daten werden Sie zu ID Austria weitergeleitet.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "Zu Entwicklungszwecken gibt es auch die Möglichkeit, Daten über einen QR-Code zu laden:",
//                modifier = Modifier.padding(horizontal = 16.dp),
//            )
        }
    }
}
