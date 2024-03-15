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
import composewalletapp.shared.generated.resources.ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_LOCATION
import composewalletapp.shared.generated.resources.ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_NAME
import composewalletapp.shared.generated.resources.BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_SUBTITLE
import composewalletapp.shared.generated.resources.BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_TITLE
import composewalletapp.shared.generated.resources.HEADING_LABEL_AUTHENTICATE_AT_DEVICE
import composewalletapp.shared.generated.resources.HEADING_LABEL_NAVIGATE_BACK
import composewalletapp.shared.generated.resources.PROMPT_ASK_LOAD_MISSING_DATA
import composewalletapp.shared.generated.resources.PROMPT_SEND_ABOVE_DATA
import composewalletapp.shared.generated.resources.PROMPT_SEND_ALL_DATA
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.SECTION_HEADING_DATA_RECIPIENT
import composewalletapp.shared.generated.resources.SECTION_HEADING_REQUESTED_DATA
import composewalletapp.shared.generated.resources.WARNING_REQUESTED_DATA_NOT_AVAILABLE_CONTENT
import composewalletapp.shared.generated.resources.WARNING_REQUESTED_DATA_NOT_AVAILABLE_HEADING
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
                        stringResource(Res.string.HEADING_LABEL_NAVIGATE_BACK),
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
                            stringResource(Res.string.PROMPT_ASK_LOAD_MISSING_DATA)
                        } else if(requestedAttributes.isNotEmpty()) {
                            stringResource(Res.string.PROMPT_SEND_ABOVE_DATA)
                        } else {
                            stringResource(Res.string.PROMPT_SEND_ALL_DATA)
                        }

                    val bottomBarContinueButton: @Composable () -> Unit = {
                        if (hasMissingAttributes) {
                            ReloadDataButton(loadMissingData)
                        } else {
                            ConsentButton(consentToDataTransmission)
                        }
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
                        bottomBarContinueButton()
                    }
                }
            }
        }
    ) {
        if (showBiometry) {
            BiometryPrompt(
                title = stringResource(Res.string.BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_TITLE),
                subtitle = "${stringResource(Res.string.BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_SUBTITLE)}: $spName",
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
                    stringResource(Res.string.HEADING_LABEL_AUTHENTICATE_AT_DEVICE),
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
                                text = stringResource(Res.string.WARNING_REQUESTED_DATA_NOT_AVAILABLE_HEADING),
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
                                    text = stringResource(Res.string.WARNING_REQUESTED_DATA_NOT_AVAILABLE_CONTENT),
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
                        title = stringResource(Res.string.SECTION_HEADING_DATA_RECIPIENT),
                        data = mapOf(
                            stringResource(Res.string.ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_NAME) to spName,
                            stringResource(Res.string.ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_LOCATION) to spLocation,
                        ).toList(),
                        modifier = paddingModifier,
                    )
                    if(requestedAttributes.isNotEmpty()) {
                        DataCategoryDisplaySection(
                            title = stringResource(Res.string.SECTION_HEADING_REQUESTED_DATA),
                            attributes = requestedAttributes,
                            modifier = paddingModifier,
                        )
                    }
                }
            }
        }
    }
}