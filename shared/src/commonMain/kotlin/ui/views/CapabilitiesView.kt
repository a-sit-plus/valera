package ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.capabilities_heading_attestation
import at.asitplus.valera.resources.capabilities_heading_internet
import at.asitplus.valera.resources.capabilities_heading_signing
import at.asitplus.valera.resources.content_description_navigate_to_settings
import at.asitplus.valera.resources.heading_label_capabilities
import at.asitplus.valera.resources.info_text_capabilities_continue
import at.asitplus.valera.resources.info_text_capabilities_missing_capabilities
import at.asitplus.valera.resources.info_text_capabilities_no_attestation
import at.asitplus.valera.resources.info_text_capabilities_no_internet
import at.asitplus.valera.resources.info_text_capabilities_no_signing
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.composables.CapabilityCard
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton
import ui.viewmodels.CapabilitiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityView(
    koinScope: Scope,
    vm: CapabilitiesViewModel = koinViewModel(scope = koinScope),
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onClickContinue: () -> Unit,
) {
    val signerStatus = vm.capabilitiesService.signerStatus.collectAsState(null)
    val attestationStatus = vm.capabilitiesService.attestationStatus.collectAsState(null)
    val onlineStatus = vm.capabilitiesService.onlineStatus.collectAsState(null)

    val workingStatus = (attestationStatus.value == true && signerStatus.value == true)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        runBlocking { vm.capabilitiesService.refreshStatus() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeading(
                        stringResource(Res.string.heading_label_capabilities),
                        Modifier.weight(1f),
                    )
                }
            }, actions = {
                Logo(onClick = onClickLogo)
                Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.content_description_navigate_to_settings),
                    )
                }
                Spacer(Modifier.width(15.dp))
            })
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    BottomAppBarDefaults.ContainerElevation
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    if (!workingStatus) {
                        Text(
                            text = stringResource(Res.string.info_text_capabilities_missing_capabilities),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(Res.string.info_text_capabilities_continue),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextIconButton(
                            icon = {},
                            text = {
                                Text(stringResource(Res.string.button_label_continue))
                            },
                            onClick = onClickContinue,
                            enabled = workingStatus
                        )
                    }
                }
            }
        }) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 15.dp, end = 15.dp),
            ) {
                signerStatus.value?.let {
                    CapabilityCard(
                        stringResource(Res.string.capabilities_heading_signing),
                        it,
                        stringResource(Res.string.info_text_capabilities_no_signing)
                    )
                }
                Spacer(Modifier.height(15.dp))
                attestationStatus.value?.let {
                    CapabilityCard(
                        stringResource(Res.string.capabilities_heading_attestation),
                        it,
                        stringResource(Res.string.info_text_capabilities_no_attestation)
                    )
                }

                Spacer(Modifier.height(15.dp))
                onlineStatus.value?.let {
                    CapabilityCard(
                        stringResource(Res.string.capabilities_heading_internet),
                        it,
                        stringResource(Res.string.info_text_capabilities_no_internet)
                    )
                }
                Spacer(Modifier.height(15.dp))
            }
        }
    }
}