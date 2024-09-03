package ui.views

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.app.common.CryptoServiceAuthorizationContext
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.attribute_friendly_name_data_recipient_location
import composewalletapp.shared.generated.resources.attribute_friendly_name_data_recipient_name
import composewalletapp.shared.generated.resources.heading_label_authenticate_at_device_screen
import composewalletapp.shared.generated.resources.heading_label_navigate_back
import composewalletapp.shared.generated.resources.info_text_submission_preview_disabled
import composewalletapp.shared.generated.resources.prompt_send_above_data
import composewalletapp.shared.generated.resources.section_heading_data_recipient
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.BiometryPrompt
import ui.composables.BiometryPromptDismissResult
import ui.composables.BiometryPromptSuccessResult
import ui.composables.DataDisplaySection
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ConsentButton
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun AuthenticationConsentView(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    navigateUp: () -> Unit,
    consentToDataTransmission: () -> Unit,
    cancelAuthentication: () -> Unit
) {
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
                    NavigateUpButton(navigateUp)
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
                        CancelButton(cancelAuthentication)
                        Spacer(modifier = Modifier.width(16.dp))
                        ConsentButton(consentToDataTransmission)
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
                    if (spImage != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = spImage,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = paddingModifier.height(64.dp),
                            )
                        }
                    }
                    DataDisplaySection(
                        title = stringResource(Res.string.section_heading_data_recipient),
                        data = mapOf(
                            stringResource(Res.string.attribute_friendly_name_data_recipient_name) to spName,
                            stringResource(Res.string.attribute_friendly_name_data_recipient_location) to spLocation,
                        ).toList(),
                        modifier = paddingModifier,
                    )

                    Text(
                        stringResource(Res.string.info_text_submission_preview_disabled),
                        modifier = Modifier.weight(1.0f, true)
                    )
                }
            }
        }
    }
}