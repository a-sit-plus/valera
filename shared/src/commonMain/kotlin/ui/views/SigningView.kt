package ui.views

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource
import ui.viewmodels.SigningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningView(
    vm: SigningViewModel
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
                vm.createSignRequest(payload)
            },
            onClickLogo = vm.onClickLogo)
    }
}