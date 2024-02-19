package view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.WalletMain
import kotlinx.coroutines.runBlocking
import ui.views.LoadDataView
import ui.views.MyDataView

@Composable
fun MyDataScreen(
    walletMain: WalletMain,
    refreshCredentials: () -> Unit,
) {
    val credentials = runBlocking { walletMain.subjectCredentialStore.getVcs() }
    if (credentials.size == 0) {
        LoadDataView(
            loadData = refreshCredentials,
            navigateUp = null,
        )
    } else {
        MyDataView(
            refreshCredentials = refreshCredentials,
            identityData = null, // TODO("Create from credential attributes")
            ageData = null, // TODO("Create from credential attributes")
            drivingData = null, // TODO("Create from credential attributes")
            navigateToIdentityData = null, // TODO("Create from credential attributes")
            navigateToAgeData = null, // TODO("Create from credential attributes")
            navigateToDrivingData = null, // TODO("Create from credential attributes")
        )
    }
}
