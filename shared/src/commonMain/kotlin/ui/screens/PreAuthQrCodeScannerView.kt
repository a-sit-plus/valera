package ui.screens

import PreAuthQrCodeScannerViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreAuthQrCodeScannerScreen(
    vm: PreAuthQrCodeScannerViewModel
) {
    val vm = remember { vm }

    if (vm.isLoading) {
        LoadingScreen()
    } else {
        GenericQrCodeScannerView(title = stringResource(Res.string.heading_label_authenticate_at_device_title),
            subTitle = null,
            navigateUp = vm.navigateUp,
            onFoundQrCode = { payload ->
                vm.isLoading = true
                vm.getCredential(payload)
            })
    }
}