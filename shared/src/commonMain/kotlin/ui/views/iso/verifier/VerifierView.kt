package ui.views.iso.verifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_check_response
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import at.asitplus.valera.resources.info_text_waiting_for_response
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.method.rememberPlatformContext
import at.asitplus.wallet.app.common.iso.transfer.state.PreconditionState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.VerifierState
import at.asitplus.wallet.app.common.iso.transfer.state.rememberTransferSettingsState
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.LoadingView
import ui.views.iso.common.MissingPreconditionView

@Composable
fun VerifierView(
    vm: VerifierViewModel,
    onError: (Throwable) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    LaunchedEffect(vm) { vm.initSettings() }

    val platformContext = rememberPlatformContext()
    val deviceTransferMethodManager = remember { DeviceTransferMethodManager() }
    val transferSettingsState =
        rememberTransferSettingsState(vm.settingsRepository, deviceTransferMethodManager)

    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val hasResumed by vm.hasResumed.collectAsStateWithLifecycle()
    val verifierState by vm.verifierState.collectAsState()
    val blePermissionState = rememberBluetoothPermissionState()

    LaunchedEffect(
        transferSettingsState.ble,
        transferSettingsState.nfc,
        settingsReady,
        hasResumed
    ) {
        if (!settingsReady) return@LaunchedEffect
        val next = when (transferSettingsState.precondition) {
            TransferPrecondition.Ok -> VerifierState.SelectDocument
            TransferPrecondition.NoTransferMethodSelected ->
                VerifierState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_SELECTED)
            TransferPrecondition.NoTransferMethodAvailable ->
                VerifierState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION)
            TransferPrecondition.MissingPermission ->
                VerifierState.MissingPrecondition(PreconditionState.MISSING_PERMISSION)
        }
        if (hasResumed) vm.resetResume()
        if (verifierState != next) vm.setState(next)
    }

    when (val state = verifierState) {
        is VerifierState.Init ->
            if (!settingsReady) {
                Napier.i("Loading transfer settings", tag = "VerifierView")
                LoadingView(
                    stringResource(Res.string.info_text_transfer_settings_loading)
                )
            } else {
                LoadingView()
            }
        is VerifierState.SelectDocument -> VerifierDocumentSelectionView(vm, bottomBar)
        is VerifierState.SelectCustomRequest -> VerifierCustomSelectionView(vm)
        is VerifierState.SelectCombinedRequest -> VerifierCombinedSelectionView(vm)
        is VerifierState.QrEngagement -> VerifierQrEngagementView(vm)
        is VerifierState.WaitingForResponse -> LoadingView(
            customLabel = stringResource(Res.string.info_text_waiting_for_response),
            navigateUp = vm.onResume
        )
        is VerifierState.CheckResponse -> LoadingView(
            customLabel = stringResource(Res.string.info_text_check_response),
            navigateUp = vm.onResume
        )
        is VerifierState.Presentation -> VerifierPresentationView(vm)
        is VerifierState.Error -> onError(vm.throwable.value!!)
        is VerifierState.MissingPrecondition -> MissingPreconditionView(
            reason = state.reason,
            transferSettingsState = transferSettingsState,
            deviceTransferMethodManager = deviceTransferMethodManager,
            platformContext = platformContext,
            blePermissionState = blePermissionState,
            onClickSettings = vm.onClickSettings,
            navigateUp = vm.navigateUp,
            onClickLogo = vm.onClickLogo,
        )
    }
}
