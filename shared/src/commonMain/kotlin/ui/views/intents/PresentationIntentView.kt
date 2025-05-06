package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.PresentationIntentViewModel
import ui.views.LoadingView

@Composable
fun PresentationIntentView(vm: PresentationIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}