package ui.views

import ui.viewmodels.PreAuthQrCodeScannerViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreAuthQrCodeScannerScreen(
    vm: PreAuthQrCodeScannerViewModel
) {
    val vm = remember { vm }

    if (vm.isLoading) {
        LoadingView()
    } else {
        GenericQrCodeScannerView(title = stringResource(Res.string.heading_label_authenticate_at_device_title),
            subTitle = null,
            navigateUp = vm.navigateUp,
            onFoundQrCode = { payload ->
                vm.isLoading = true
                vm.getCredential(payload)
            },
            onClickLogo = vm.onClickLogo)
    }
}