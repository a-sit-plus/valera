package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.SigningCredentialIntentViewModel
import ui.views.LoadingView

@Composable
fun SigningCredentialIntentView(vm: SigningCredentialIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}