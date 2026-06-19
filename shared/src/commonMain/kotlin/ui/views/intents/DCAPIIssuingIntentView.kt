package ui.views.intents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import at.asitplus.wallet.app.common.LoadingMessageKey
import ui.viewmodels.intents.DCAPIIssuingIntentViewModel
import ui.views.LoadingView
import ui.views.loadingMessageString

@Composable
fun DCAPIIssuingIntentView(vm: DCAPIIssuingIntentViewModel) {
    LaunchedEffect(null) {
        vm.process()
    }
    LoadingView(loadingMessageString(LoadingMessageKey.CredentialOffer))
}
