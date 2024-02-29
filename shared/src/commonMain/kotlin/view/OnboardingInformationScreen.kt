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
import ui.composables.TextIcon
import ui.composables.buttons.ContinueButton


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
                            text = Resources.INFO_TEXT_ENTHUSIASTIC_WELCOME_END,
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
                            TextIcon(Resources.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_ICON_TEXT)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Resources.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_TITLE,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        gapSpacer()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIcon(Resources.ONBOARDING_SECTION_LOAD_DATA_ICON_TEXT)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Resources.ONBOARDING_SECTION_LOAD_DATA_TITLE,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = Resources.ONBOARDING_SECTION_LOAD_DATA_SUBTITLE,
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
                                TextIcon(Resources.ONBOARDING_SECTION_SHOW_DATA_ICON_TEXT)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = Resources.ONBOARDING_SECTION_SHOW_DATA_TITLE,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = Resources.ONBOARDING_SECTION_SHOW_DATA_SUBTITLE,
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
                                    TextIcon(Resources.ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_ICON_TEXT)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_TITLE,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_SUBTITLE,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    TextIcon(Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_ICON_TEXT)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_TITLE,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_SUBTITLE,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    TextIcon(Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_ICON_TEXT)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_TITLE,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = Resources.ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_SUBTITLE,
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
