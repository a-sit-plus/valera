package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_authenticate_at_device_subtitle
import at.asitplus.valera.resources.heading_label_authenticate_at_device_title
import at.asitplus.valera.resources.heading_label_sign
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.navigation.routes.Route
import ui.viewmodels.QrCodeScannerMode
import ui.viewmodels.QrCodeScannerViewModel

@Composable
fun QrCodeScannerView(
    onNavigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onNavigateToRoute: (Route) -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    vm: QrCodeScannerViewModel = koinViewModel(scope = koinScope),
) {
    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }

    if (isLoading) {
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
            navigateUp = onNavigateUp,
            onFoundQrCode = { payload ->
                isLoading = true
                vm.onQrScanned(
                    payload,
                    onSuccess = onNavigateToRoute,
                    onFailure = onError,
                )
            },
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings
        )
    }
}