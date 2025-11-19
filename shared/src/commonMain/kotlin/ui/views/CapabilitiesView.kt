package ui.views

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.capabilities_heading_attestation
import at.asitplus.valera.resources.capabilities_heading_biometry
import at.asitplus.valera.resources.capabilities_heading_camera
import at.asitplus.valera.resources.capabilities_heading_device_lock
import at.asitplus.valera.resources.capabilities_heading_internet
import at.asitplus.valera.resources.capabilities_heading_signing
import at.asitplus.valera.resources.info_text_capabilities_no_attestation
import at.asitplus.valera.resources.info_text_capabilities_no_attestation_ios
import at.asitplus.valera.resources.info_text_capabilities_no_biometry_enrolled
import at.asitplus.valera.resources.info_text_capabilities_no_camera
import at.asitplus.valera.resources.info_text_capabilities_no_device_lock_set
import at.asitplus.valera.resources.info_text_capabilities_no_internet
import at.asitplus.valera.resources.info_text_capabilities_no_signing
import at.asitplus.wallet.app.common.CapabilitiesData
import getPlatformName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.navigation.routes.RoutePrerequisites
import ui.navigation.routes.RoutePrerequisites.*
import ui.viewmodels.CapabilitiesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CapabilityView(
    koinScope: Scope,
    vm: CapabilitiesViewModel = koinViewModel(scope = koinScope),
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onSoftReset: () -> Unit,
    onContinue: () -> Unit,
    onNavigateUp: () -> Unit,
    prerequisites: Set<RoutePrerequisites>
) {
    val capabilitiesData by vm.capabilitiesService.getDeviceStatus().collectAsState(null)
    val evaluation by vm.capabilitiesService.evaluatePrerequisites(prerequisites).collectAsState(null)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        CoroutineScope(Dispatchers.IO).launch {
            vm.capabilitiesService.refreshStatus()
        }
    }

    capabilitiesData?.let { data ->
        val statusData = prepareCapabilityCards(
            prerequisites,
            data,
            { vm.walletMain.platformAdapter.openDeviceSettings() }
        )

        if (prerequisites.contains(CRYPTO) &&
            (!data.deviceLockStatus || !data.biometryEnrolledStatus)
        ) {
            vm.needReset = true
        }

        val callback = if (vm.needReset == true) onSoftReset else onContinue
        val navigateUp = if (vm.needReset == true) null else onNavigateUp

        if (evaluation == true) {
            LaunchedEffect(Unit) { callback() }
        }

        when {
            prerequisites.contains(CAMERA) && data.cameraPermission == null -> {
                LoadingView()
                RequestCameraPermission()
            }

            evaluation == false -> {
                CapabilitiesCardView(statusData, onClickLogo, onClickSettings, navigateUp)
            }

            else -> LoadingView()
        }
    } ?: LoadingView()
}

data class CapabilityCardData(
    val text: String, val success: Boolean, val info: String, val action: (() -> Unit)?
)

@Composable
fun prepareCapabilityCards(
    prerequisites: Set<RoutePrerequisites>, capabilitiesData: CapabilitiesData, openDeviceSettings: () -> Unit
): Set<CapabilityCardData> {
    val set = mutableSetOf<CapabilityCardData>()
    prerequisites.forEach {
        when (it) {
            CRYPTO -> {
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_device_lock),
                        success = capabilitiesData.deviceLockStatus,
                        info = stringResource(Res.string.info_text_capabilities_no_device_lock_set),
                        action = openDeviceSettings
                    )
                )
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_biometry),
                        success = capabilitiesData.biometryEnrolledStatus,
                        info = stringResource(Res.string.info_text_capabilities_no_biometry_enrolled),
                        action = openDeviceSettings
                    )
                )
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_attestation),
                        success = capabilitiesData.attestationStatus,
                        info = when (getPlatformName()) {
                            "iOS" -> {
                                stringResource(Res.string.info_text_capabilities_no_attestation) + " " + stringResource(
                                    Res.string.info_text_capabilities_no_attestation_ios
                                )
                            }

                            else -> {
                                stringResource(Res.string.info_text_capabilities_no_attestation)
                            }
                        },
                        action = null
                    )
                )
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_signing),
                        success = capabilitiesData.signingStatus,
                        info = stringResource(Res.string.info_text_capabilities_no_signing),
                        action = openDeviceSettings
                    )
                )
            }

            INTERNET -> {
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_internet),
                        success = capabilitiesData.onlineStatus,
                        info = stringResource(Res.string.info_text_capabilities_no_internet),
                        action = null
                    )
                )
            }

            CAMERA -> {
                set.add(
                    CapabilityCardData(
                        text = stringResource(Res.string.capabilities_heading_camera),
                        success = capabilitiesData.cameraPermission == true,
                        info = stringResource(Res.string.info_text_capabilities_no_camera),
                        action = openDeviceSettings
                    )
                )
            }
        }
    }
    return set
}