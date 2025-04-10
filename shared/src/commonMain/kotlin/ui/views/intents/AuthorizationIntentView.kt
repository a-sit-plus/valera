package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.AuthorizationIntentViewModel
import ui.views.LoadingView

@Composable
fun AuthorizationIntentView(vm: AuthorizationIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}