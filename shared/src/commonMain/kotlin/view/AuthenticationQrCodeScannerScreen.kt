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
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParameters
import navigation.AuthenticationConsentPage
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

data class RequestResponse(
    val redirectUri: String,
    val requestParameters: AuthenticationRequestParameters,
)

@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    navigateToLoadingScreen: () -> Unit,
    navigateToConsentScreen: (AuthenticationConsentPage) -> Unit,
    authenticationQrCodeScannerViewModel: AuthenticationQrCodeScannerViewModel,
    walletMain: WalletMain,
) = AuthenticationQrCodeScannerView(
    navigateUp = navigateUp,
    onFoundPayload = { link ->
        authenticationQrCodeScannerViewModel.onScan(
            link = link,
            startLoadingCallback = navigateToLoadingScreen,
            stopLoadingCallback = navigateUp,
            onFailure = { throwable ->
                walletMain.errorService.emit(throwable)
            },
            onSuccess = { page ->
                navigateToConsentScreen(page)
            },
        )
    }
)

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