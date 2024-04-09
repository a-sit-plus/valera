package ui.screens

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
import composewalletapp.shared.generated.resources.heading_label_authenticate_at_device_subtitle
import composewalletapp.shared.generated.resources.heading_label_authenticate_at_device_title
import composewalletapp.shared.generated.resources.Res
import ui.navigation.AuthenticationConsentPage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView
import view.AuthenticationQrCodeScannerViewModel

@Composable
fun AuthenticationQrCodeScannerScreen(
    navigateUp: () -> Unit,
    showNavigateUpButton: Boolean = true,
    navigateToLoadingScreen: () -> Unit,
    navigateToConsentScreen: (AuthenticationConsentPage) -> Unit,
    walletMain: WalletMain,
    authenticationQrCodeScannerViewModel: AuthenticationQrCodeScannerViewModel,
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

fun TestAuthenticationQrCodeScannerScreen(
    navigateToLoadingScreen: () -> Unit,
    navigateUp: () -> Unit,
    navigateToConsentScreen: (AuthenticationConsentPage) -> Unit,
    model: AuthenticationQrCodeScannerViewModel
) {
    model.onScan(
        link = "https://wallet.a-sit.at/mobile?request_uri=https://apps.egiz.gv.at/terminal_sp/siopv2/request&client_id=https://apps.egiz.gv.at/terminal_sp/siopv2/postsuccess&client_metadata_uri=https://apps.egiz.gv.at/terminal_sp/siopv2/metadata",
        //link = "https://oe1.orf.at/intro?gad_source=1&gclid=EAIaIQobChMIgtms7dS0hQMV6E5BAh2nnQL6EAAYASAAEgIPdPD_BwE",
        startLoadingCallback = navigateToLoadingScreen,
        stopLoadingCallback = navigateUp,
        onFailure = { throwable ->
            //    walletMain.errorService.emit(throwable)
        },
        onSuccess = { page ->
            navigateUp()
            //br
            navigateToConsentScreen(page)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun AuthenticationQrCodeScannerView(
    onFoundPayload: (String) -> Unit,
    navigateUp: (() -> Unit)? = null,
) {
    //bp
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(Res.string.heading_label_authenticate_at_device_title),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            stringResource(Res.string.heading_label_authenticate_at_device_subtitle),
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
                //bp
                onFoundPayload = onFoundPayload,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}