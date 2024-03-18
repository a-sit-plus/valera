package ui.screens

import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.launch
import ui.views.LoadDataView


@Composable
fun LoadDataScreen(
    navigateUp: () -> Unit,
    walletMain: WalletMain,
) {
    LoadDataView(
        loadData = {
            walletMain.scope.launch {
                try {
                    walletMain.provisioningService.startProvisioning()
                    navigateUp()
                } catch (e: Exception) {
                    walletMain.errorService.emit(e)
                }
            }
        },
        navigateUp = navigateUp,
    )
}