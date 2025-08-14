package ui.views.iso

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import at.asitplus.valera.resources.button_label_retry
import at.asitplus.valera.resources.error_bluetooth_and_nfc_unavailable
import at.asitplus.valera.resources.error_missing_permissions
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.valera.resources.info_text_qr_code_loading
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import at.asitplus.wallet.app.common.iso.transfer.MdocHelper
import at.asitplus.wallet.app.common.iso.transfer.rememberTransferSettingsState
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
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.authentication.PresentationStateModel
import ui.viewmodels.iso.ShowQrCodeState
import ui.viewmodels.iso.ShowQrCodeViewModel
import ui.views.LoadingViewBody
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeView(
    navigateUp: () -> Unit,
    onNavigateToPresentmentScreen: (PresentationStateModel) -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onError: (Throwable) -> Unit,
    koinScope: Scope,
    vm: ShowQrCodeViewModel = koinViewModel(scope = koinScope),
) {
    val TAG = "ShowQrCodeView"

    LaunchedEffect(vm) { vm.initSettings() }

    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val transferSettingsState = rememberTransferSettingsState(vm.settingsRepository)
    val blePermissionState = rememberBluetoothPermissionState()

    val showQrCode = remember { mutableStateOf<ByteString?>(null) }
    val presentationStateModel = remember { vm.presentationStateModel }
    val showQrCodeState by vm.showQrCodeState.collectAsState()

    LaunchedEffect( transferSettingsState.bleSettingOn, transferSettingsState.nfcSettingOn) {
        showQrCode.value = null
        vm.hasBeenCalledHack = false
        vm.setState(ShowQrCodeState.INIT)
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
                when (showQrCodeState) {
                    ShowQrCodeState.INIT -> {
                        if (!settingsReady) {
                            Napier.i("Loading transfer settings", tag = TAG)
                            LoadingViewBody(scaffoldPadding, stringResource(Res.string.info_text_transfer_settings_loading))
                        } else {
                            if (!transferSettingsState.transferMethodAvailableForCurrentSettings) {
                                vm.setState(ShowQrCodeState.NO_TRANSFER_METHOD_AVAILABLE)
                                return@Box
                            } else if (showQrCode.value != null && presentationStateModel.state.collectAsState().value != PresentationStateModel.State.PROCESSING) {
                                vm.setState(ShowQrCodeState.SHOW_QR_CODE)
                                return@Box
                            } else if (transferSettingsState.missingRequiredBlePermission) {
                                vm.setState(ShowQrCodeState.MISSING_PERMISSION)
                                return@Box
                                // TODO: add case for missing nfc permission
                            } else {
                                vm.setState(ShowQrCodeState.CREATE_ENGAGEMENT)
                                return@Box
                            }
                        }
                    }

                    ShowQrCodeState.NO_TRANSFER_METHOD_AVAILABLE -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(Res.string.error_bluetooth_and_nfc_unavailable))
                            TextIconButton(
                                icon = { Icons.Default.Repeat },
                                text = { Text(stringResource(Res.string.button_label_retry)) },
                                onClick = { vm.setState(ShowQrCodeState.INIT) }
                            )
                        }
                    }

                    // TODO: add handling for missing nfc permission
                    ShowQrCodeState.MISSING_PERMISSION -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(Res.string.error_missing_permissions))
                            LaunchedEffect(Unit) {
                                blePermissionState.launchPermissionRequest()
                            }
                            TextIconButton(
                                icon = { Icons.Default.Repeat },
                                text = { Text(stringResource(Res.string.button_label_retry)) },
                                onClick = { vm.setState(ShowQrCodeState.INIT) }
                            )
                            // TODO handle case when user needs to go to settings application to grant permission
                        }
                    }

                    ShowQrCodeState.CREATE_ENGAGEMENT -> {
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
                                transferSettingsState.bleRequired
                            )
                            vm.doHolderFlow(
                                showQrCode,
                                transferSettingsState.isBleEnabled,
                                transferSettingsState.isNfcEnabled
                            ) { error ->
                                when {
                                    error == null -> onNavigateToPresentmentScreen(vm.presentationStateModel)
                                    error is CancellationException && error.message?.contains("PresentationModel reset") == true -> {
                                        Napier.i("PresentationModel reset\nThis may happen when changing transfer settings after the presentation state model has been setup", tag = TAG)
                                    }
                                    else -> onError(error)
                                }
                            }
                        }
                    }

                    ShowQrCodeState.SHOW_QR_CODE -> {
                        val qrBytes = showQrCode.value
                        if (qrBytes == null) {
                            LaunchedEffect(vm.presentationStateModel.presentmentScope) {
                                vm.setState(ShowQrCodeState.INIT)
                            }
                            return@Box
                        }
                        Image(
                            painter = rememberQrCodePainter(
                                data = MdocHelper.buildDeviceEngagementQrCode(qrBytes),
                                colors = if (isSystemInDarkTheme()) {
                                    QrColors(dark = QrBrush.solid(Color.White))
                                } else QrColors()
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.8f)
                        )
                    }

                    ShowQrCodeState.FINISHED -> { LoadingViewBody(scaffoldPadding) }
                }
            }
        }
    }
}
