package view

import Resources
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.buttons.ContinueButton
import ui.composables.TextAvatar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInformationScreen(
    onClickContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Resources.HEADING_LABEL_INFORMATION,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ContinueButton(onClickContinue)
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            Scaffold(
                bottomBar = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    ) {
                        Text(
                            text = "Daten laden und los geht's!",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            ) { innerScaffoldPadding ->
                Column(
                    modifier = Modifier.padding(innerScaffoldPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    val gapSpacer: @Composable () -> Unit = {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    ) {
                        val gapSpacer: @Composable () -> Unit = {
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextAvatar(
                                text = "1",
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nutzungsbedingungen und Datenschutz",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        gapSpacer()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextAvatar(
                                text = "2",
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daten in die App laden",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "mit einmaligem Kontakt zum zentralen ID Austria",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        gapSpacer()
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextAvatar(
                                    text = "3",
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Daten vorzeigen",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "direkt aus dem Speicher der App",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.padding(start = 32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    TextAvatar(
                                        text = "A",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "am Schalter oder bei Maschinen",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "bspw. bei Amtswegen",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    TextAvatar(
                                        text = "B",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "gegenüber der Exekutive",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "bspw. bei Verkehrskontrollen",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    TextAvatar(
                                        text = "C",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "gegenüber dritten Bürgern",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "bspw. Veranstaltungen",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
