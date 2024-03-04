package view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.launch
import ui.views.LoadDataView
import ui.views.MyCredentialsView

@Composable
fun MyCredentialsScreen(
    navigateToRefreshCredentialsPage: () -> Unit,
    walletMain: WalletMain,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer()
        .collectAsState(null)

    storeContainerState?.let { storeContainer ->
        if (storeContainer.credentials.isEmpty()) {
            LoadDataView(
                loadData = {
                    walletMain.scope.launch {
                        try {
                            walletMain.provisioningService.startProvisioning()
                        } catch (e: Exception) {
                            walletMain.errorService.emit(e)
                        }
                    }
                },
                navigateUp = null,
            )
        } else {
            MyCredentialsView(
                credentials = storeContainer.credentials,
                onRefreshCredentials = navigateToRefreshCredentialsPage,
                decodeImage = walletMain.platformAdapter::decodeImage,
            )
        }
    }
}
