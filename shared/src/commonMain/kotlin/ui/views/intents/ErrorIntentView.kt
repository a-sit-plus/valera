package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.ErrorIntentViewModel
import ui.views.LoadingView

@Composable
fun ErrorIntentView(vm: ErrorIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}