package view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.wallet.app.common.WalletMain
import ui.views.LoadDataView
import ui.views.MyDataView

@Composable
fun MyDataScreen(
    walletMain: WalletMain,
    refreshCredentials: () -> Unit,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer().collectAsState(null)
    val credentialRepresentationFlow by walletMain.walletConfig.credentialRepresentation.collectAsState(null)

    storeContainerState?.let { storeContainer ->
        credentialRepresentationFlow?.let { credentialRepresentation ->
            if (storeContainer.credentials.isEmpty()) {
                LoadDataView(
                    loadData = refreshCredentials,
                    navigateUp = null,
                )
            } else {
                MyDataView(
                    refreshCredentials = refreshCredentials,
                    identityData = null, // TODO("Create from credential attributes")
                    navigateToIdentityData = null, // TODO("Create from credential attributes")
                    ageData = null, // TODO("Create from credential attributes")
                    navigateToAgeData = null, // TODO("Create from credential attributes")
                    drivingData = null, // TODO("Create from credential attributes")
                    navigateToDrivingData = null, // TODO("Create from credential attributes")
                )
            }
        }
    }
}
