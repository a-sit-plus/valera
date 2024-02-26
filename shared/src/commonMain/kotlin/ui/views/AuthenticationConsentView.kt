package ui.views

import Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
import navigation.Page
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.composables.AttributeAvailability
import ui.composables.DataCategoryDisplaySection
import ui.composables.DataDisplaySection
import ui.composables.PersonalDataCategory
import ui.composables.buttons.CancelButton
import ui.composables.buttons.ConsentButton
import ui.composables.buttons.LoadDataButton
import ui.composables.buttons.NavigateUpButton

class AuthenticationConsentPage(
    val spName: String,
    val spLocation: String,
    val requestedAttributes: List<Pair<PersonalDataCategory, List<AttributeAvailability>>>,
) : Page

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun AuthenticationConsentView(
    navigateUp: () -> Unit,
    cancelAuthentication: () -> Unit,
    loadMissingData: () -> Unit,
    consentToDataTransmission: () -> Unit,
    spName: String,
    spLocation: String,
    spImage: ImageBitmap?,
    requestedAttributes: List<Pair<PersonalDataCategory, List<AttributeAvailability>>>,
    showBottomSheet: Boolean,
    bottomSheetState: SheetState,
    onBottomSheetDismissRequest: () -> Unit,
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
                        } else {
                            "Sollen diese Daten übermittelt werden?"
                        }

                    val bottomBarContinueButton: @Composable () -> Unit = {
                        if (hasMissingAttributes) {
                            LoadDataButton(loadMissingData)
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
                    .verticalScroll(state = rememberScrollState()),
            ) {
                val paddingModifier = Modifier.padding(bottom = 32.dp)
                Text(
                    "Anmelden an\nSchalter oder Maschine",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = paddingModifier,
                )
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
                            modifier = Modifier.padding(top = 0.dp, end = 16.dp, bottom = 8.dp, start = 16.dp)
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
                if(spImage != null) {
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
                DataCategoryDisplaySection(
                    title = "Angefragte Daten",
                    attributes = requestedAttributes,
                    modifier = paddingModifier,
                )
            }
        }
        // source: https://stackoverflow.com/questions/66014834/how-to-draw-a-circular-image-in-android-jetpack-compose
//        Image(
//            painter = painterResource(R.drawable.sample_avatar),
//            contentDescription = "avatar",
//            contentScale = ContentScale.Crop,            // crop the image if it's not a square
//            modifier = Modifier.clip(CircleShape)                       // clip to the circle shape
//        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = onBottomSheetDismissRequest,
                sheetState = bottomSheetState,
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Biometrie entsperren")
                    }

                }
            }
        }
    }
}
