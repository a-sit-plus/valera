package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import at.asitplus.wallet.app.common.LoadingMessageKey
import ui.viewmodels.intents.ProvisioningIntentViewModel
import ui.views.LoadingView
import ui.views.loadingMessageString

@Composable
fun ProvisioningIntentView(vm: ProvisioningIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView(loadingMessageString(LoadingMessageKey.IssuingCredential))
}
