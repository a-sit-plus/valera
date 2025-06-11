package ui.views

import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_subtitle
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import at.asitplus.valera.resources.heading_label_sign
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.QrCodeScannerMode
import ui.viewmodels.QrCodeScannerViewModel

@Composable
fun QrCodeScannerView(
    vm: QrCodeScannerViewModel
) {
    if (vm.isLoading) {
        LoadingView()
    } else {
        val (title, subtitle) = when (vm.mode) {
            QrCodeScannerMode.SIGNING -> Pair(stringResource(Res.string.heading_label_sign), null)
            QrCodeScannerMode.AUTHENTICATION -> Pair(
                stringResource(Res.string.heading_label_authenticate_at_device_title),
                stringResource(Res.string.heading_label_authenticate_at_device_subtitle)
            )
            QrCodeScannerMode.PROVISIONING -> Pair(
                stringResource(Res.string.heading_label_authenticate_at_device_title),
                null
            )
        }

        GenericQrCodeScannerView(
            title = title,
            subTitle = subtitle,
            navigateUp = vm.navigateUp,
            onFoundQrCode = { payload ->
                vm.isLoading = true
                vm.onQrScanned(payload)
            },
            onClickLogo = vm.onClickLogo,
            onClickSettings = vm.onClickSettings
        )
    }
}