package ui.views.Authentication

import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_navigate_back
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.section_heading_data_recipient
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ConsentAttributesSection
import ui.composables.DataDisplaySection
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ContinueButton
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.Authentication.AuthenticationConsentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationConsentView(vm: AuthenticationConsentViewModel) {
    val vm = remember { vm }

    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_navigate_back),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Image(
                            modifier = Modifier.padding(start = 0.dp, end = 0.dp, top = 8.dp),
                            painter = painterResource(Res.drawable.asp),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
        },
        bottomBar = {
            Surface(
                color = NavigationBarDefaults.containerColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.prompt_send_above_data),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CancelButton(vm.navigateUp)
                        Spacer(modifier = Modifier.width(16.dp))
                        ContinueButton(vm.consentToDataTransmission)
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val paddingModifier = Modifier.padding(bottom = 32.dp)
                Text(
                    stringResource(Res.string.heading_label_authenticate_at_device_screen),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = paddingModifier,
                )

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()),
                ) {
                    if (vm.spImage != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = vm.spImage,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = paddingModifier.height(64.dp),
                            )
                        }
                    }
                    DataDisplaySection(
                        title = stringResource(Res.string.section_heading_data_recipient),
                        data = listOfNotNull(
                            vm.spName?.let { stringResource(Res.string.attribute_friendly_name_data_recipient_name) to vm.spName },
                            stringResource(Res.string.attribute_friendly_name_data_recipient_location) to vm.spLocation,
                        ),
                        modifier = paddingModifier,
                    )
                    vm.requests.mapNotNull { it.value }.forEach { params ->
                        params.resolved?.first?.let { scheme ->
                            val schemeName = scheme.uiLabel()
                            val format = params.resolved?.second?.name
                            val attributes = params.attributes?.mapNotNull {
                                scheme.getLocalization(NormalizedJsonPath() + it)
                            }
                            if (format != null && attributes != null) {
                                ConsentAttributesSection(
                                    title = "$schemeName (${format})",
                                    list = attributes
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}