package ui.views.iso.holder

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_check_settings
import at.asitplus.valera.resources.info_text_qr_code_loading
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.method.rememberBluetoothEnabledState
import at.asitplus.wallet.app.common.iso.transfer.method.rememberPlatformContext
import at.asitplus.wallet.app.common.iso.transfer.state.HolderState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.evaluateTransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.rememberTransferSettingsState
import at.asitplus.wallet.app.common.iso.transfer.state.toEnum
import io.github.aakira.napier.Napier
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.iso.holder.HolderViewModel
import ui.views.LoadingView
import ui.views.iso.common.MissingPreconditionView
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolderView(
    navigateUp: () -> Unit,
    onNavigateToPresentmentScreen: (PresentationStateModel) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    vm: HolderViewModel = koinViewModel(scope = koinScope)
) {
    val TAG = "HolderView"

    val platformContext = rememberPlatformContext()
    val deviceTransferMethodManager = remember {
        DeviceTransferMethodManager(platformContext, vm.walletMain.platformAdapter)
    }

    val bluetoothPermissionState = rememberBluetoothPermissionState()
    val bluetoothEnabledState = rememberBluetoothEnabledState()
    val transferSettingsState = rememberTransferSettingsState(vm.settingsRepository)

    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val holderState by vm.holderState.collectAsState()
    val presentationState by remember(vm.presentationStateModel) {
        vm.presentationStateModel.state
    }.collectAsStateWithLifecycle()

    val showQrCode = remember { mutableStateOf<ByteString?>(null) }

    LaunchedEffect(transferSettingsState) {
//        showQrCode.value = null
        vm.hasBeenCalledHack = false
        vm.setState(HolderState.Settings)
    }

    if (holderState !is HolderState.Settings) {
        LaunchedEffect(
            transferSettingsState,
            bluetoothEnabledState.isEnabled,
            bluetoothPermissionState.isGranted,
            settingsReady,
            showQrCode.value,
            presentationState
        ) {
            if (!settingsReady) return@LaunchedEffect
            val next = when (
                val transferPrecondition = evaluateTransferPrecondition(
                    transferSettingsState =transferSettingsState,
                    bleEnabled = bluetoothEnabledState.isEnabled,
                    blePermissionGranted = bluetoothPermissionState.isGranted,
                    nfcEnabled = true,
                    nfcEngagementSelected = false
                )
            ) {
                TransferPrecondition.Ok -> when {
                    showQrCode.value != null && presentationState != PresentationStateModel.State.PROCESSING ->
                        HolderState.ShowQrCode
                    else -> HolderState.CreateEngagement
                }
                else -> HolderState.MissingPrecondition(transferPrecondition.toEnum())
            }
            if (holderState != next) vm.setState(next)
        }
    }


    when (val state = holderState) {
        is HolderState.Settings ->
            HolderSettingsView(navigateUp, onClickLogo, onClickSettings, bottomBar, vm)

        is HolderState.CheckSettings -> LoadingView(
            customLabel = stringResource(Res.string.info_text_check_settings),
            navigateUp = vm.onResume
        )

        is HolderState.MissingPrecondition -> MissingPreconditionView(
            reason = state.reason,
            transferSettingsState = transferSettingsState,
            deviceTransferMethodManager = deviceTransferMethodManager,
            bluetoothPermissionState = bluetoothPermissionState,
            onClickSettings = onClickSettings,
            navigateUp = vm.onResume,
            onClickLogo = onClickLogo,
            onClickBackToSettings = vm.onResume
        )

        is HolderState.CreateEngagement -> {
            Napier.i("Create Engagement", tag = TAG)
            LoadingView(
                customLabel = stringResource(Res.string.info_text_qr_code_loading),
                navigateUp = vm.onResume
            )
            LaunchedEffect(holderState) {
                if (vm.hasBeenCalledHack) return@LaunchedEffect
                vm.hasBeenCalledHack = true
                vm.setupPresentmentModel(
                    bluetoothPermissionState,
                    transferSettingsState.ble.required
                )
                vm.doHolderFlow(
                    showQrCode,
                    bluetoothEnabledState.isEnabled,
                    true
                ) { error ->
                    handleError(error, vm, onNavigateToPresentmentScreen, onError)
                }
            }
        }

        is HolderState.ShowQrCode -> HolderShowQrCodeView(
            bytes = showQrCode.value,
            onClickLogo = onClickLogo,
            onClickSettings = onClickSettings,
            vm = vm,
            bottomBar = bottomBar,
            onError = onError
        )
        is HolderState.Finished -> LoadingView() // TODO: add finished screen ?
    }
}

private fun handleError(
    error: Throwable?,
    vm: HolderViewModel,
    onNavigateToPresentmentScreen: (PresentationStateModel) -> Unit,
    onError: (Throwable) -> Unit
) {
    when {
        error == null -> onNavigateToPresentmentScreen(vm.presentationStateModel)
        error is CancellationException &&
                error.message?.contains("PresentationModel reset") == true -> {
            Napier.i(
                "PresentationModel reset — may happen when changing transfer settings after setup",
                tag = "ShowQrCodeView"
            )
        }
        else -> onError(error)
    }
}
