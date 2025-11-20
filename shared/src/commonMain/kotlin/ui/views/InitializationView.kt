package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.viewmodels.InitializationViewModel

@Composable
fun InitializationView(
    koinScope: Scope,
    viewModel: InitializationViewModel = koinInject(scope = koinScope),
    navigateOnboarding: () -> Unit,
    navigateHomeScreen: () -> Unit,
) {

    val isConditionsAccepted = viewModel.walletMain.settingsRepository.isConditionsAccepted.collectAsState(null)

    when (isConditionsAccepted.value) {
        false -> navigateOnboarding()
        true -> navigateHomeScreen()
        null -> {}
    }
    LoadingView()
}