package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.SigningPreloadIntentViewModel
import ui.views.LoadingView

@Composable
fun SigningPreloadIntentView(vm: SigningPreloadIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}