package ui.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import navigation.Page


class ShowDataPage : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDataView(
    navigateToAuthenticationAtSp: () -> Unit,
    navigateToShowDataToExecutive: () -> Unit,
    navigateToShowDataToOtherCitizen: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Daten Vorzeigen",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(modifier = Modifier.padding(horizontal = 8.dp).verticalScroll(state = rememberScrollState())) {
                val paddingModifier = Modifier.padding(top = 8.dp)

                Text("In welcher Situation möchten Sie Ihre Daten vorzeigen?")

                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Anmelden an")
                        Text(
                            "Schalter oder Maschine",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Button(
                                onClick = navigateToAuthenticationAtSp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Anmelden",
                                )
                                Text("Anmelden")
                            }
                        }
                    }
                }
                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Daten vorzeigen an")
                        Text(
                            "Exekutive",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Button(
                                onClick = navigateToShowDataToExecutive
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Daten an Exekutive vorzeigen",
                                )
                                Text("Vorzeigen")
                            }
                        }
                    }
                }
                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Daten vorzeigen an")
                        Text(
                            "dritte Bürger",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Button(
                                onClick = navigateToShowDataToOtherCitizen
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Daten an andere Bürger vorzeigen",
                                )
                                Text("Vorzeigen")
                            }
                        }
                    }
                }
            }
        }
    }
}
