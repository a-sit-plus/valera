package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_check_response
import at.asitplus.valera.resources.info_text_check_settings
import at.asitplus.valera.resources.info_text_waiting_for_response
import at.asitplus.wallet.app.common.iso.transfer.method.rememberAppSettings
import at.asitplus.wallet.app.common.iso.transfer.method.rememberNfcEnabledState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.VerifierState
import at.asitplus.wallet.app.common.iso.transfer.state.evaluateTransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.rememberTransferSettingsState
import at.asitplus.wallet.app.common.iso.transfer.state.toEnum
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import org.multipaz.compose.permissions.rememberBluetoothEnabledState
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.LoadingView
import ui.views.iso.common.MissingPreconditionView

@Composable
fun VerifierView(
    navigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
    bottomBar: @Composable () -> Unit,
    koinScope: Scope,
    vm: VerifierViewModel = koinViewModel(scope = koinScope)
) {
    val bluetoothPermissionState = rememberBluetoothPermissionState()
    val bluetoothEnabledState = rememberBluetoothEnabledState()
    val nfcEnabledState = rememberNfcEnabledState()
    val transferSettingsState = rememberTransferSettingsState(vm.settingsRepository)
    val appSettings = rememberAppSettings()

    val verifierState by vm.verifierState.collectAsState()

    if (verifierState !is VerifierState.Settings) {
        LaunchedEffect(
            transferSettingsState,
            bluetoothEnabledState.isEnabled,
            bluetoothPermissionState.isGranted,
            nfcEnabledState.isEnabled
        ) {
            val next = when (
                val transferPrecondition = evaluateTransferPrecondition(
                    transferSettingsState =transferSettingsState,
                    bleEnabled = bluetoothEnabledState.isEnabled,
                    blePermissionGranted = bluetoothPermissionState.isGranted,
                    nfcEnabled = nfcEnabledState.isEnabled,
                    engagementMethod = vm.selectedEngagementMethod.value
                )
            ) {
                TransferPrecondition.Ok -> VerifierState.SelectDocument
                else -> VerifierState.MissingPrecondition(transferPrecondition.toEnum())
            }
            if (verifierState != next) vm.setState(next)
        }
    }

    when (val state = verifierState) {
        is VerifierState.Settings ->
            VerifierSettingsView(onClickLogo, onClickSettings, bottomBar, vm)
        is VerifierState.CheckSettings -> LoadingView(
            customLabel = stringResource(Res.string.info_text_check_settings),
            navigateUp = vm.onResume
        )
        is VerifierState.SelectDocument ->
            VerifierDocumentSelectionView(onClickLogo, onClickSettings, vm, bottomBar)
        is VerifierState.SelectCustomRequest ->
            VerifierCustomSelectionView(onClickLogo, onClickSettings, vm)
        is VerifierState.SelectCombinedRequest ->
            VerifierCombinedSelectionView(onClickLogo, onClickSettings, vm)
        is VerifierState.QrEngagement -> VerifierQrEngagementView(onClickLogo, vm)
        is VerifierState.WaitingForResponse -> LoadingView(
            customLabel = stringResource(Res.string.info_text_waiting_for_response),
            navigateUp = vm.onResume
        )
        is VerifierState.CheckResponse -> LoadingView(
            customLabel = stringResource(Res.string.info_text_check_response),
            navigateUp = vm.onResume
        )
        is VerifierState.Presentation ->
            VerifierPresentationView(navigateUp, onClickLogo, vm)
        is VerifierState.Error -> onError(vm.throwable.value!!)
        is VerifierState.MissingPrecondition -> MissingPreconditionView(
            reason = state.reason,
            bluetoothPermissionState = bluetoothPermissionState,
            bluetoothEnabledState = bluetoothEnabledState,
            nfcEnabledState = nfcEnabledState,
            onClickSettings = onClickSettings,
            navigateUp = vm.onResume,
            onClickLogo = onClickLogo,
            onClickBackToSettings = vm.onResume,
            onOpenAppSettings = { appSettings.open() }
        )
    }
}
