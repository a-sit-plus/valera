package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ui.viewmodels.intents.DCAPICreationIntentViewModel
import ui.views.LoadingView

@Composable
fun DCAPICreationIntentView(vm: DCAPICreationIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView()
}
