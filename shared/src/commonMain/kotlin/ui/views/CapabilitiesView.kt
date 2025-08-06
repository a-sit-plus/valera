package ui.views

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.capabilities_heading_attestation
import at.asitplus.valera.resources.capabilities_heading_internet
import at.asitplus.valera.resources.capabilities_heading_signing
import at.asitplus.valera.resources.content_description_navigate_to_settings
import at.asitplus.valera.resources.heading_label_capabilities
import at.asitplus.valera.resources.info_text_no_attestation
import at.asitplus.valera.resources.info_text_no_internet
import at.asitplus.valera.resources.info_text_no_signing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.scope.Scope
import ui.composables.CapabilityCard
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.viewmodels.CapabilitiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityView(
    koinScope: Scope,
    vm: CapabilitiesViewModel = koinViewModel(scope = koinScope),
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
) {
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
        }) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 15.dp, end = 15.dp),
            ) {
                CapabilityCard(
                    stringResource(Res.string.capabilities_heading_signing), vm.getSignerCheck(), stringResource(Res.string.info_text_no_signing)
                )
                Spacer(Modifier.height(15.dp))
                CapabilityCard(
                    stringResource(Res.string.capabilities_heading_attestation),
                    vm.getAttestationCheck(),
                    stringResource(Res.string.info_text_no_attestation)
                )
                Spacer(Modifier.height(15.dp))
                CapabilityCard(
                    stringResource(Res.string.capabilities_heading_internet), vm.isOnlineCheck(), stringResource(Res.string.info_text_no_internet)
                )
                Spacer(Modifier.height(15.dp))
            }
        }
    }
}