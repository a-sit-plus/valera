package ui.views.iso.verifier

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.heading_label_select_data_retrieval_screen
import at.asitplus.valera.resources.heading_label_settings_screen
import at.asitplus.valera.resources.info_text_transfer_settings_loading
import at.asitplus.valera.resources.section_heading_request_engagement_method
import at.asitplus.wallet.app.common.iso.transfer.method.DeviceEngagementMethods
import at.asitplus.wallet.app.common.iso.transfer.method.rememberBluetoothEnabledState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.permissions.rememberBluetoothPermissionState
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.TextIconButton
import ui.viewmodels.iso.verifier.VerifierViewModel
import ui.views.LoadingView
import ui.views.iso.common.SingleChoiceButton
import ui.views.iso.common.TransferOptionsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifierSettingsView(
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
    vm: VerifierViewModel
) {
    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    val selectedEngagementMethod by vm.selectedEngagementMethod.collectAsState()

    LaunchedEffect(vm) { vm.initSettings() }

    val blePermissionState = rememberBluetoothPermissionState()
    val bleEnabledState = rememberBluetoothEnabledState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ScreenHeading(stringResource(Res.string.heading_label_select_data_retrieval_screen))
                            Text(
                                text = stringResource(Res.string.heading_label_settings_screen),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                    Spacer(Modifier.width(15.dp))
                }
            )
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
//        if (!blePermissionState.isGranted) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Button(
//                    onClick = {
//                        vm.walletMain.scope.launch {
//                            blePermissionState.launchPermissionRequest()
//                        }
//                    }
//                ) {
//                    Text("Request BLE permissions")
//                }
//            }
//        }  else if (!bleEnabledState.isEnabled) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Button(
//                    onClick = {
//                        vm.walletMain.scope.launch {
//                            bleEnabledState.enable()
//                        }
//                    }
//                ) {
//                    Text("Enable Bluetooth")
//                }
//            }
        if (!settingsReady) {
            LoadingView(stringResource(Res.string.info_text_transfer_settings_loading))
        } else {
            Box(modifier = Modifier.padding(scaffoldPadding)) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(Res.string.section_heading_request_engagement_method),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    DeviceEngagementMethods.entries.forEach { engagementMethod ->
                        SingleChoiceButton(
                            current = engagementMethod.friendlyName,
                            selectedOption = selectedEngagementMethod.friendlyName,
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                            icon = { Icon(engagementMethod.icon, null) }
                        ) { vm.setEngagementMethod(engagementMethod) }
                    }

                    Spacer(Modifier.height(24.dp))
                    TransferOptionsView( vm)

                    Spacer(Modifier.height(24.dp))
                    TextIconButton(
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                        text = { Text(stringResource(Res.string.button_label_continue)) },
                        onClick = { vm.onConsentSettings() }
                    )
                }
            }
        }
    }
}
