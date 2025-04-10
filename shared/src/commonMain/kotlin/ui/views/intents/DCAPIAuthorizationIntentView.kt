package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.DCAPIAuthorizationIntentViewModel
import ui.views.LoadingView

@Composable
fun DCAPIAuthorizationIntentView(vm: DCAPIAuthorizationIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}