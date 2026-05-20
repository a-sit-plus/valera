package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_check_scan_qr_code
import org.jetbrains.compose.resources.stringResource
import org.koin.core.scope.Scope
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.QrCodeScannerScreen

@Composable
fun VerifierQrEngagementView(
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUp: () -> Unit,
    vm: VerifierViewModel,
    koinScope: Scope,
) {
    QrCodeScannerScreen(
        title = stringResource(Res.string.heading_label_check_scan_qr_code),
        subTitle = null,
        navigateUp = navigateUp,
        onFoundQrCode = vm.onFoundPayload,
        onClickLogo = onClickLogo,
        onClickSettings = onClickSettings,
        koinScope = koinScope,
        walletMain = vm.walletMain,
    )
}
