package view

import Resources
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
                        Resources.HEADING_LABEL_SHOW_DATA,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).verticalScroll(state = rememberScrollState())) {
            Column(modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)) {
                val paddingModifier = Modifier.padding(top = 24.dp)

                Text(Resources.INFO_TEXT_SHOW_DATA_SITUATION)

                ElevatedCard(
                    modifier = paddingModifier.fillMaxWidth(),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                Resources.SECTION_HEADING_AUTHENTICATE_AT_DEVICE_TITLE,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                Resources.SECTION_HEADING_AUTHENTICATE_AT_DEVICE_SUBTITLE,
                                style = MaterialTheme.typography.bodySmall,
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
                                        contentDescription = null,
                                    )
                                },
                                text = {
                                    Text(Resources.BUTTON_LABEL_AUTHENTICATE)
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
                                Resources.SECTION_HEADING_SHOW_DATA_TO_EXECUTIVE_TITLE,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                Resources.SECTION_HEADING_SHOW_DATA_TO_EXECUTIVE_SUBTITLE,
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
                                        contentDescription = null,
                                    )
                                },
                                text = {
                                    Text(Resources.BUTTON_LABEL_SHOW_DATA)
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
                                Resources.SECTION_HEADING_SHOW_DATA_TO_OTHER_CITIZEN_TITLE,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                Resources.SECTION_HEADING_SHOW_DATA_TO_OTHER_CITIZEN_SUBTITLE,
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
                                        contentDescription = null,
                                    )
                                },
                                text = {
                                    Text(Resources.BUTTON_LABEL_SHOW_DATA)
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
