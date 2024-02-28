package view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import ui.composables.TextIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDataScreen(
    navigateToAuthenticationStartPage: () -> Unit,
    onClickShowDataToExecutive: () -> Unit,
    onClickShowDataToOtherCitizen: () -> Unit,
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
        Column(modifier = Modifier.padding(it).verticalScroll(state = rememberScrollState())) {
            Column(modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)) {
                val paddingModifier = Modifier.padding(top = 24.dp)

                Text("In welcher Situation möchten Sie Ihre Daten vorzeigen?")

                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth(),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Anmelden an",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Schalter oder Maschine",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Anmelden",
                                    )
                                },
                                text = {
                                    Text("Anmelden")
                                },
                                onClick = navigateToAuthenticationStartPage,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth(),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Daten vorzeigen an",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Exekutive",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Daten an Exekutive vorzeigen",
                                    )
                                },
                                text = {
                                    Text("Vorzeigen")
                                },
                                onClick = onClickShowDataToExecutive,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth(),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Daten vorzeigen an",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "dritte Bürger",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Daten an andere Bürger vorzeigen",
                                    )
                                },
                                text = {
                                    Text("Vorzeigen")
                                },
                                onClick = onClickShowDataToOtherCitizen,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
