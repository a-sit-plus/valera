package ui.screens

import PreAuthQrCodeScannerViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton
import ui.views.CameraView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreAuthQrCodeScannerScreen(
    vm: PreAuthQrCodeScannerViewModel
) {
    var isLoading by remember {vm.isLoading}


    if (isLoading) {
        LoadingScreen()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                stringResource(Res.string.heading_label_authenticate_at_device_title),
                                style = MaterialTheme.typography.headlineLarge,
                            )
                        }
                    },
                    navigationIcon = { NavigateUpButton({
                        vm.isLoading.value = true
                    }) },
                )
            },
        ) {
            Column(modifier = Modifier.padding(it).fillMaxSize()) {
                CameraView(
                    onFoundPayload = { payload ->
                        isLoading = true
                        vm.getCredential(payload)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}