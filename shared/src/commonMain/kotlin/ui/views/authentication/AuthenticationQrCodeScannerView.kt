package ui.views.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_subtitle
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ui.navigation.routes.AuthenticationViewRoute
import ui.viewmodels.authentication.AuthenticationQrCodeScannerViewModel
import ui.views.GenericQrCodeScannerView
import ui.views.LoadingView

@Composable
fun AuthenticationQrCodeScannerView(
    navigateUp: (() -> Unit)?,
    onSuccess: (AuthenticationViewRoute) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    // TODO: replace with koinViewModel as soon as we figure out how to amend instrumented tests
    vm: AuthenticationQrCodeScannerViewModel = koinInject(),
) {
    var isLoading by mutableStateOf(false)
    if (isLoading) {
        LoadingView()
    } else {
        GenericQrCodeScannerView(
            title = stringResource(Res.string.heading_label_authenticate_at_device_title),
            subTitle = stringResource(Res.string.heading_label_authenticate_at_device_subtitle),
            navigateUp = navigateUp,
            onFoundQrCode = { payload ->
                isLoading = true
                vm.onScan(
                    payload,
                    onSuccess = {
                        isLoading = false
                        onSuccess(it)
                    },
                    onFailure = {
                        isLoading = false
                    }
                )
            },
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
        )
    }
}