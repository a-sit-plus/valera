package ui.views.iso.holder

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.valera.resources.info_text_qr_code_loading
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceTransferMethodManager
import at.asitplus.wallet.app.common.iso.transfer.method.rememberPlatformContext
import at.asitplus.wallet.app.common.iso.transfer.state.PreconditionState
import at.asitplus.wallet.app.common.iso.transfer.state.HolderState
import at.asitplus.wallet.app.common.iso.transfer.state.TransferPrecondition
import at.asitplus.wallet.app.common.iso.transfer.state.rememberTransferSettingsState
import io.github.aakira.napier.Napier
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import org.multipaz.util.toBase64Url
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.iso.holder.HolderViewModel
import ui.views.LoadingViewBody
import ui.views.iso.common.MissingPreconditionViewBody
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

    LaunchedEffect(vm) { vm.initSettings() }

    val platformContext = rememberPlatformContext()
    val deviceTransferMethodManager = remember {
        DeviceTransferMethodManager(platformContext, vm.walletMain.platformAdapter)
    }
    val transferSettingsState =
        rememberTransferSettingsState(vm.settingsRepository, deviceTransferMethodManager)

    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val showQrCodeState by vm.showQrCodeState.collectAsState()
    val presentationState by remember(vm.presentationStateModel) {
        vm.presentationStateModel.state
    }.collectAsStateWithLifecycle()

    val blePermissionState = rememberBluetoothPermissionState()
    val showQrCode = remember { mutableStateOf<ByteString?>(null) }

    LaunchedEffect(transferSettingsState.ble, transferSettingsState.nfc) {
        showQrCode.value = null
        vm.hasBeenCalledHack = false
        vm.setState(HolderState.Init)
    }

    LaunchedEffect(
        transferSettingsState.ble,
        transferSettingsState.nfc,
        settingsReady,
        showQrCode.value,
        presentationState
    ) {
        if (!settingsReady) return@LaunchedEffect
        val next = when (transferSettingsState.precondition) {
            TransferPrecondition.Ok -> when {
                showQrCode.value != null && presentationState != PresentationStateModel.State.PROCESSING ->
                    HolderState.ShowQrCode
                else -> HolderState.CreateEngagement
            }
            TransferPrecondition.NoTransferMethodSelected ->
                HolderState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_SELECTED)
            TransferPrecondition.NoTransferMethodAvailable ->
                HolderState.MissingPrecondition(PreconditionState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION)
            TransferPrecondition.MissingPermission ->
                HolderState.MissingPrecondition(PreconditionState.MISSING_PERMISSION)
        }
        if (showQrCodeState != next) vm.setState(next)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScreenHeading(
                            stringResource(Res.string.heading_label_show_qr_code_screen),
                            Modifier.weight(1f),
                        )
                    }
                },
                navigationIcon = { NavigateUpButton(navigateUp) },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val state = showQrCodeState) {
                    is HolderState.Init ->
                        if (!settingsReady) {
                            Napier.i("Loading transfer settings", tag = TAG)
                            LoadingViewBody(
                                scaffoldPadding,
                                stringResource(Res.string.info_text_transfer_settings_loading)
                            )
                        } else {
                            LoadingViewBody(scaffoldPadding)
                        }

                    is HolderState.Settings ->
                        HolderSettingsView(onClickLogo, onClickSettings, bottomBar, vm)

                    is HolderState.CheckSettings -> {}

                    is HolderState.MissingPrecondition -> MissingPreconditionViewBody(
                        reason = state.reason,
                        transferSettingsState = transferSettingsState,
                        deviceTransferMethodManager = deviceTransferMethodManager,
                        blePermissionState = blePermissionState,
                        onClickSettings = onClickSettings,
                    )

                    is HolderState.CreateEngagement -> {
                        Napier.i("Create Engagement", tag = TAG)
                        LoadingViewBody(
                            scaffoldPadding,
                            stringResource(Res.string.info_text_qr_code_loading)
                        )
                        LaunchedEffect(showQrCodeState) {
                            if (vm.hasBeenCalledHack) return@LaunchedEffect
                            vm.hasBeenCalledHack = true
                            vm.setupPresentmentModel(
                                blePermissionState,
                                transferSettingsState.ble.required
                            )
                            vm.doHolderFlow(
                                showQrCode,
                                transferSettingsState.ble.enabled,
                                transferSettingsState.nfc.enabled
                            ) { error ->
                                handleError(error, vm, onNavigateToPresentmentScreen, onError)
                            }
                        }
                    }

                    is HolderState.ShowQrCode -> QrCodeView(showQrCode.value)
                    is HolderState.Finished -> LoadingViewBody(scaffoldPadding)
                }
            }
        }
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

@Composable
private fun QrCodeView(bytes: ByteString?) {
    val data = bytes ?: return
    Image(
        painter = rememberQrCodePainter(
            data = MdocConstants.MDOC_PREFIX + data.toByteArray().toBase64Url(),
            colors = if (isSystemInDarkTheme()) {
                QrColors(dark = QrBrush.solid(Color.White))
            } else QrColors()
        ),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(0.8f)
    )
}
