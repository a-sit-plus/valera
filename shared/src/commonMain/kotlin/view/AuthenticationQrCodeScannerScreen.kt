package view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import at.asitplus.wallet.app.common.WalletMain
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Anmelden",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            "an Schalter oder Maschine",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(navigateUp)
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            CameraView(
                onFoundPayload = { payload ->
                    TODO()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}