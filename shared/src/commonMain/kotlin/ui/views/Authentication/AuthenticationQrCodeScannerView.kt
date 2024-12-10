package ui.views.Authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_authenticate_at_device_subtitle
import compose_wallet_app.shared.generated.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.Authentication.AuthenticationQrCodeScannerViewModel
import ui.views.GenericQrCodeScannerView
import ui.views.LoadingView

@Composable
fun AuthenticationQrCodeScannerView(
    vm: AuthenticationQrCodeScannerViewModel
) {
    val vm = remember { vm }

    if (vm.isLoading) {
        LoadingView()
    } else {
        GenericQrCodeScannerView(title = stringResource(Res.string.heading_label_authenticate_at_device_title),
            subTitle = stringResource(Res.string.heading_label_authenticate_at_device_subtitle),
            navigateUp = vm.navigateUp,
            onFoundQrCode = { payload -> vm.onScan(payload) })
    }
}