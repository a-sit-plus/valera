package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.ProvisioningIntentViewModel
import ui.views.LoadingView

@Composable
fun ProvisioningIntentView(vm: ProvisioningIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}