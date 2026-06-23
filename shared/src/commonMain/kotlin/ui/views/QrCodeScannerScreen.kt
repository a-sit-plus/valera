package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.wallet.app.common.WalletMain
import org.koin.core.scope.Scope
import ui.navigation.routes.RoutePrerequisites

@Composable
fun QrCodeScannerScreen(
    title: String,
    subTitle: String?,
    navigateUp: (() -> Unit)?,
    onFoundQrCode: (String) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    koinScope: Scope,
    walletMain: WalletMain,
) {
    val capabilitiesData by walletMain.capabilitiesService.getDeviceStatus().collectAsState(null)
    val hasCameraPermission = capabilitiesData?.cameraPermission == true

    if (hasCameraPermission) {
        GenericQrCodeScannerView(
            title = title,
            subTitle = subTitle,
            navigateUp = navigateUp,
            onFoundQrCode = onFoundQrCode,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
        )
    } else {
        CapabilityView(
            koinScope = koinScope,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            onContinue = {},
            onNavigateUp = navigateUp ?: {},
            prerequisites = setOf(RoutePrerequisites.CAMERA),
        )
    }
}
