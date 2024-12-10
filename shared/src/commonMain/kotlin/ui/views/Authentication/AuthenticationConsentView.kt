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
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.third_party.at.asitplus.wallet.lib.data.getLocalization
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_data_recipient_location
import compose_wallet_app.shared.generated.resources.attribute_friendly_name_data_recipient_name
import compose_wallet_app.shared.generated.resources.heading_label_authenticate_at_device_screen
import compose_wallet_app.shared.generated.resources.heading_label_navigate_back
import compose_wallet_app.shared.generated.resources.prompt_send_above_data
import compose_wallet_app.shared.generated.resources.section_heading_data_recipient
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
                    Text(
                        stringResource(Res.string.heading_label_navigate_back),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    NavigateUpButton(vm.navigateUp)
                },
            )
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
                    vm.requests.forEach { request ->
                        val params = request.value
                        val scheme = params.resolved?.first
                        val schemeName =
                            scheme?.let { it.sdJwtType ?: it.isoDocType ?: it.vcType } ?: scheme?.schemaUri
                        val format = params.resolved?.second?.name
                        val attributes = request.value.attributes?.mapNotNull {
                            scheme?.getLocalization(NormalizedJsonPath() + it)
                        }
                        if (format != null && attributes != null) {
                            ConsentAttributesSection(
                                title = "${schemeName} (${format})",
                                list = attributes
                            )
                        }
                    }
                }
            }
        }
    }
}