package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.SigningIntentViewModel
import ui.views.LoadingView

@Composable
fun SigningIntentView(vm: SigningIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}