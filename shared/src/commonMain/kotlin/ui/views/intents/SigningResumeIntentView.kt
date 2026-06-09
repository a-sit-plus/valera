package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.SigningResumeIntentViewModel
import ui.views.LoadingView

@Composable
fun SigningResumeIntentView(vm: SigningResumeIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}
