package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.DCAPIIssuingIntentViewModel
import ui.views.LoadingView

@Composable
fun DCAPIIssuingIntentView(vm: DCAPIIssuingIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}
