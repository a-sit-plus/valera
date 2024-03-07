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
import domain.RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase
import domain.RetrieveRequestRedirectFromAuthenticationQrCodeUseCase
import navigation.AuthenticationConsentPage
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    showNavigateUpButton: Boolean = true,
    navigateToLoadingScreen: () -> Unit,
    navigateToConsentScreen: (AuthenticationConsentPage) -> Unit,
    walletMain: WalletMain,
    authenticationQrCodeScannerViewModel: AuthenticationQrCodeScannerViewModel = AuthenticationQrCodeScannerViewModel(
        retrieveRequestRedirectFromAuthenticationQrCodeUseCase = RetrieveRequestRedirectFromAuthenticationQrCodeUseCase(
            client = walletMain.httpService.buildHttpClient()
        ),
        retrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase = RetrieveRelyingPartyMetadataFromAuthenticationQrCodeUseCase(
            client = walletMain.httpService.buildHttpClient()
        ),
    ),
) = AuthenticationQrCodeScannerView(
    navigateUp = if(showNavigateUpButton) navigateUp else null,
    onFoundPayload = { link ->
        authenticationQrCodeScannerViewModel.onScan(
            link = link,
            startLoadingCallback = navigateToLoadingScreen,
            stopLoadingCallback = navigateUp,
            onFailure = { throwable ->
                walletMain.errorService.emit(throwable)
            },
            onSuccess = { page ->
                navigateUp()
                navigateToConsentScreen(page)
            },
        )
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationQrCodeScannerView(
    onFoundPayload: (String) -> Unit,
    navigateUp: (() -> Unit)? = null,
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
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                navigationIcon = {
                    if(navigateUp != null) {
                        NavigateUpButton(navigateUp)
                    }
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