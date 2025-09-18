package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_check_response
import at.asitplus.valera.resources.info_text_waiting_for_response
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.method.rememberPlatformContext
import at.asitplus.wallet.app.common.iso.transfer.state.PreconditionState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.VerifierState
import at.asitplus.wallet.app.common.iso.transfer.state.rememberTransferSettingsState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
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
    val platformContext = rememberPlatformContext()
    val deviceTransferMethodManager = remember { DeviceTransferMethodManager() }
    val transferSettingsState =
        rememberTransferSettingsState(vm.settingsRepository, deviceTransferMethodManager)

    val verifierState by vm.verifierState.collectAsState()
    val blePermissionState = rememberBluetoothPermissionState()

    when (val state = verifierState) {
        is VerifierState.Settings ->
            VerifierSettingsView(onClickLogo, onClickSettings, bottomBar, vm)
        is VerifierState.CheckSettings -> {
            LoadingView(
                "Check Settings", vm.onResume
            )
            val next = when (transferSettingsState.precondition) {
                TransferPrecondition.Ok -> VerifierState.SelectDocument
                TransferPrecondition.NoTransferMethodSelected ->
                    VerifierState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_SELECTED)

                TransferPrecondition.NoTransferMethodAvailable ->
                    VerifierState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION)

                TransferPrecondition.MissingPermission ->
                    VerifierState.MissingPrecondition(PreconditionState.MISSING_PERMISSION)
            }
            if (verifierState != next) vm.setState(next)
        }
        is VerifierState.Init -> LoadingView()
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
            transferSettingsState = transferSettingsState,
            deviceTransferMethodManager = deviceTransferMethodManager,
            platformContext = platformContext,
            blePermissionState = blePermissionState,
            onClickSettings = onClickSettings,
            navigateUp = vm.onResume,
            onClickLogo = onClickLogo,
            platformAdapter = vm.walletMain.platformAdapter
        )
    }
}
