package ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import org.koin.core.scope.Scope
import ui.viewmodels.InitializationState
import ui.viewmodels.InitializationViewModel

@Composable
fun InitializationView(
    koinScope: Scope,
    viewModel: InitializationViewModel = koinInject(scope = koinScope),
    navigateOnboarding: () -> Unit,
    navigateHomeScreen: () -> Unit,
    navigateCapabilities: () -> Unit
) {

    val isConditionsAccepted = viewModel.walletMain.settingsRepository.isConditionsAccepted.collectAsState(null)
    val signerStatus = viewModel.capabilitiesService.signerStatus.collectAsState(null)
    val attestationStatus = viewModel.capabilitiesService.attestationStatus.collectAsState(null)
    val onlineStatus = viewModel.capabilitiesService.onlineStatus.collectAsState(null)

    if (isConditionsAccepted.value != null && signerStatus.value != null && attestationStatus.value != null && onlineStatus.value != null) {
        when (viewModel.getInitializationState(
            isConditionsAccepted.value == true,
            signerStatus.value == true,
            attestationStatus.value == true,
            true
        )) {
            InitializationState.ONBOARDING -> navigateOnboarding()
            InitializationState.HOMESCREEN -> navigateHomeScreen()
            InitializationState.CAPABILITIES -> navigateCapabilities()
        }
    }
    LoadingView()
}