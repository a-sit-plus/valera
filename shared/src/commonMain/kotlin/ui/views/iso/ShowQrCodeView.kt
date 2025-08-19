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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_go_to_app_settings
import at.asitplus.valera.resources.button_label_go_to_device_settings
import at.asitplus.valera.resources.button_label_retry
import at.asitplus.valera.resources.error_missing_permissions
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.valera.resources.info_text_no_transfer_method_available_for_selection
import at.asitplus.valera.resources.info_text_no_transfer_method_selected
import at.asitplus.valera.resources.info_text_qr_code_loading
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import at.asitplus.wallet.app.common.iso.transfer.CapabilityManager
import at.asitplus.wallet.app.common.iso.transfer.MdocHelper
import at.asitplus.wallet.app.common.iso.transfer.rememberPlatformContext
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
    val capabilityManager = CapabilityManager()
    val transferSettingsState = rememberTransferSettingsState(vm.settingsRepository, capabilityManager)
    val blePermissionState = rememberBluetoothPermissionState()
    val platformContext = rememberPlatformContext()

    val showQrCode = remember { mutableStateOf<ByteString?>(null) }
    val presentationStateModel = remember { vm.presentationStateModel }
    val presentationState by presentationStateModel.state.collectAsStateWithLifecycle()
    val showQrCodeState by vm.showQrCodeState.collectAsState()

    LaunchedEffect(
        transferSettingsState.bleSettingOn,
        transferSettingsState.nfcSettingOn,
        transferSettingsState.isBleEnabled,
        transferSettingsState.isNfcEnabled
    ) {
        showQrCode.value = null
        vm.hasBeenCalledHack = false
        vm.setState(ShowQrCodeState.INIT)
    }

    LaunchedEffect(
        settingsReady,
        transferSettingsState.isAnyTransferMethodSettingOn,
        transferSettingsState.transferMethodAvailableForCurrentSettings,
        transferSettingsState.missingRequiredBlePermission,
        showQrCode.value,
        presentationState
    ) {
        if (!settingsReady) return@LaunchedEffect
        val next = when {
            !transferSettingsState.isAnyTransferMethodSettingOn -> ShowQrCodeState.NO_TRANSFER_METHOD_SELECTED

            !transferSettingsState.transferMethodAvailableForCurrentSettings -> ShowQrCodeState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION

            transferSettingsState.missingRequiredBlePermission -> ShowQrCodeState.MISSING_PERMISSION // TODO: add NFC permission

            showQrCode.value != null && presentationState != PresentationStateModel.State.PROCESSING -> ShowQrCodeState.SHOW_QR_CODE

            else -> ShowQrCodeState.CREATE_ENGAGEMENT
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
                when (showQrCodeState) {
                    ShowQrCodeState.INIT -> {
                        if (!settingsReady) {
                            Napier.i("Loading transfer settings", tag = TAG)
                            LoadingViewBody(
                                scaffoldPadding,
                                stringResource(Res.string.info_text_transfer_settings_loading)
                            )
                        } else LoadingViewBody(scaffoldPadding)
                    }

                    ShowQrCodeState.NO_TRANSFER_METHOD_SELECTED -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.info_text_no_transfer_method_selected),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextIconButton(
                                icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null
                                )
                            }, text = {
                                Text(stringResource(Res.string.button_label_go_to_app_settings))
                            }, onClick = onClickSettings
                            )
                        }
                    }

                    ShowQrCodeState.NO_TRANSFER_METHOD_AVAILABLE_FOR_SELECTION -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.info_text_no_transfer_method_available_for_selection),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextIconButton(
                                icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null
                                )
                            }, text = {
                                Text(stringResource(Res.string.button_label_go_to_app_settings))
                            }, onClick = onClickSettings
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextIconButton(
                                icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null
                                )
                            },
                                text = { Text(stringResource(Res.string.button_label_go_to_device_settings)) },
                                onClick = {
                                    if (transferSettingsState.nfcRequired) {
                                        capabilityManager.goToNfcSettings(platformContext)
                                    } else {
                                        capabilityManager.goToBluetoothSettings(platformContext)
                                    }
                                })
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
                                icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = null
                                )
                            },
                                text = { Text(stringResource(Res.string.button_label_retry)) },
                                onClick = { vm.setState(ShowQrCodeState.INIT) })
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
