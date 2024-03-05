package view

import Resources
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
import io.github.aakira.napier.Napier
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    AuthenticationQrCodeScannerView(
        navigateUp = navigateUp,
        onFoundPayload = { payload ->
            navigateUp()
            Napier.d("onScan: $payload")
            walletMain.platformAdapter.openUrl(payload)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationQrCodeScannerView(
    navigateUp: () -> Unit,
    onFoundPayload: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            Resources.HEADING_LABEL_AUTHENTICATE_AT_DEVICE_TITLE,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            Resources.HEADING_LABEL_AUTHENTICATE_AT_DEVICE_SUBTITLE,
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
                onFoundPayload = onFoundPayload,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}