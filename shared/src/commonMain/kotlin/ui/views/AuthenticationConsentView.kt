package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.attribute_friendly_name_data_recipient_location
import composewalletapp.shared.generated.resources.attribute_friendly_name_data_recipient_name
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import composewalletapp.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import composewalletapp.shared.generated.resources.heading_label_authenticate_at_device_screen
import composewalletapp.shared.generated.resources.heading_label_navigate_back
import composewalletapp.shared.generated.resources.prompt_ask_load_missing_data
import composewalletapp.shared.generated.resources.prompt_send_above_data
import composewalletapp.shared.generated.resources.prompt_send_all_data
import composewalletapp.shared.generated.resources.section_heading_data_recipient
import composewalletapp.shared.generated.resources.section_heading_requested_data
import composewalletapp.shared.generated.resources.warning_requested_data_not_available_content
import composewalletapp.shared.generated.resources.warning_requested_data_not_available_heading
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeAvailability
import ui.composables.BiometryPrompt
import ui.composables.BiometryPromptDismissResult
import ui.composables.BiometryPromptSuccessResult
import ui.composables.DataCategoryDisplaySection
import ui.composables.DataDisplaySection
import ui.composables.PersonalDataCategory
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ConsentButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.buttons.ReloadDataButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun AuthenticationConsentView(
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    requestedAttributes: List<Pair<PersonalDataCategory, List<AttributeAvailability>>>,
    navigateUp: () -> Unit,
    loadMissingData: () -> Unit,
    consentToDataTransmission: () -> Unit,
    cancelAuthentication: () -> Unit,
    showBiometry: Boolean,
    onBiometrySuccess: (BiometryPromptSuccessResult) -> Unit,
    onBiometryDismissed: (BiometryPromptDismissResult) -> Unit,
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
                val hasMissingAttributes = requestedAttributes.any {
                    it.second.any {
                        it.isAvailable == false
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    val bottomBarQuestion =
                        if (hasMissingAttributes) {
                            stringResource(Res.string.prompt_ask_load_missing_data)
                        } else if (requestedAttributes.isNotEmpty()) {
                            stringResource(Res.string.prompt_send_above_data)
                        } else {
                            stringResource(Res.string.prompt_send_all_data)
                        }

                    val bottomBarContinueButton: @Composable RowScope.() -> Unit = {
                        ReloadDataButton(loadMissingData)
                        ConsentButton(consentToDataTransmission)
                    }

                    Text(
                        text = bottomBarQuestion,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CancelButton(cancelAuthentication)
                        Spacer(modifier = Modifier.width(16.dp))
                        ReloadDataButton(loadMissingData)
                        Spacer(modifier = Modifier.width(16.dp))
                        ConsentButton(consentToDataTransmission)
                    }
                }
            }
        }
    ) {
        if (showBiometry) {
            BiometryPrompt(
                title = stringResource(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title),
                subtitle = "${stringResource(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle)}: $spName",
                onSuccess = onBiometrySuccess,
                onDismiss = onBiometryDismissed,
            )
        }
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
                    val hasMissingAttributes = requestedAttributes.any {
                        it.second.any {
                            it.isAvailable == false
                        }
                    }
                    if (hasMissingAttributes) {
                        ElevatedCard(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = paddingModifier,
                        ) {
                            Text(
                                text = stringResource(Res.string.warning_requested_data_not_available_heading),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier.padding(
                                    top = 0.dp,
                                    end = 16.dp,
                                    bottom = 8.dp,
                                    start = 16.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.warning_requested_data_not_available_content),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
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
                    if (requestedAttributes.isNotEmpty()) {
                        DataCategoryDisplaySection(
                            title = stringResource(Res.string.section_heading_requested_data),
                            attributes = requestedAttributes,
                            modifier = paddingModifier,
                        )
                    }
                }
            }
        }
    }
}