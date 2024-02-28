package ui.views

import Resources
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
import ui.composables.AttributeAvailability
import ui.composables.BiometryPrompt
import ui.composables.DataCategoryDisplaySection
import ui.composables.DataDisplaySection
import ui.composables.PersonalDataCategory
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ConsentButton
import ui.composables.buttons.NavigateUpButton
import ui.composables.buttons.ReloadDataButton

@OptIn(ExperimentalMaterial3Api::class)
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
    onBiometrySuccess: () -> Unit,
    onBiometryDismissed: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Resources.HEADING_LABEL_NAVIGATE_BACK,
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
                            "Sollen die fehlenden Daten nachgeladen werden?"
                        } else if(requestedAttributes.isNotEmpty()) {
                            "Sollen diese Daten übermittelt werden?"
                        } else {
                            "Sollen alle Daten übermittelt werden?"
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
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val paddingModifier = Modifier.padding(bottom = 32.dp)
                Text(
                    "Anmelden an\nSchalter oder Maschine",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = paddingModifier,
                )

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()),
                ) {
                    if (showBiometry) {
                        BiometryPrompt(
                            onSuccess = onBiometrySuccess,
                            onDismiss = onBiometryDismissed,
                        )
                    }

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
                                text = "Angefragte Daten nicht verfügbar",
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
                                    text = "Nicht alle angefragten Daten wurden bereits in die App geladen.",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fehlende Daten können über ID Austria nachgeladen werden.",
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
                        title = "Empfänger",
                        data = mapOf(
                            "Name" to spName,
                            "Ort" to spLocation,
                        ).toList(),
                        modifier = paddingModifier,
                    )
                    if(requestedAttributes.isNotEmpty()) {
                        DataCategoryDisplaySection(
                            title = "Angefragte Daten",
                            attributes = requestedAttributes,
                            modifier = paddingModifier,
                        )
                    }
                }
            }
        }
    }
}