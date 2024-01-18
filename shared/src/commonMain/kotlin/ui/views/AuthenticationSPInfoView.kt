package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import navigation.Page
import ui.composables.DataDisplaySection
import ui.composables.OutlinedTextIconButton
import ui.composables.TextIconButton
import ui.defaults.ButtonDefaultOverrides


class AuthenticationSPInfoPage(
    val spName: String,
    val spLocation: String,
) : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationSPInfoView(
    navigateUp: () -> Unit,
    cancelAuthentication: () -> Unit,
    authenticateAtSp: () -> Unit,
    spName: String,
    spLocation: String,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Zurück",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
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
                        text = "Wollen Sie sich bei diesem Empfänger anmelden?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        OutlinedTextIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Abbrechen",
                                )
                            },
                            text = {
                                Text("Abbrechen")
                            },
                            contentPadding = ButtonDefaultOverrides.ContentPadding.TextIconButton,
                            onClick = cancelAuthentication,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Weiter",
                                )
                            },
                            text = {
                                Text(
                                    "Weiter",
                                    textAlign = TextAlign.Center,
                                )
                            },
                            onClick = authenticateAtSp,
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.padding(it)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(state = rememberScrollState()),
            ) {
                val paddingModifier = Modifier.padding(bottom = 32.dp)
                Text(
                    "Anmelden an\nSchalter oder Maschine",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = paddingModifier,
                )
                DataDisplaySection(
                    title = "Empfänger",
                    data = mapOf(
                        "Name" to spName,
                        "Ort" to spLocation,
                    ).toList(),
                    modifier = paddingModifier,
                )
            }
        }
//        Column(modifier = Modifier.padding(it)) {
//            Column(modifier = Modifier.padding(8.dp)) {
//                Text(
//                    "Empfänger",
//                    style = MaterialTheme.typography.labelLarge,
//                    color = MaterialTheme.colorScheme.secondary,
//                    fontWeight = FontWeight.SemiBold,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                Column(
//                    modifier = Modifier.padding(start = 16.dp)
//                ) {
//                    LabeledText(
//                        text = spName,
//                        label = "Name",
//                        modifier = Modifier.padding(bottom = 4.dp),
//                    )
//                    LabeledText(
//                        text = spLocation,
//                        label = "Ort",
//                        modifier = Modifier.padding(bottom = 4.dp),
//                    )
//                }
//            }
//        }
    }
}