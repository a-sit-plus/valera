package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.SigningServiceIntentViewModel
import ui.views.LoadingView

@Composable
fun SigningServiceIntentView(vm: SigningServiceIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}