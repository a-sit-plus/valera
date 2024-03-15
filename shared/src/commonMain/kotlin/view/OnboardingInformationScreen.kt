package view

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
import composewalletapp.shared.generated.resources.HEADING_LABEL_INFORMATION
import composewalletapp.shared.generated.resources.INFO_TEXT_ENTHUSIASTIC_WELCOME_END
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_LOAD_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_LOAD_DATA_SUBTITLE
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_LOAD_DATA_TITLE
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_SHOW_DATA_ICON_TEXT
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_SHOW_DATA_SUBTITLE
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_SHOW_DATA_TITLE
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_ICON_TEXT
import composewalletapp.shared.generated.resources.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_TITLE
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIcon
import ui.composables.buttons.ContinueButton


@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun OnboardingInformationScreen(
    onClickContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.HEADING_LABEL_INFORMATION),
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
                            text = stringResource(Res.string.INFO_TEXT_ENTHUSIASTIC_WELCOME_END),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            ) { innerScaffoldPadding ->
                Column(
                    modifier = Modifier.padding(innerScaffoldPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    val textIconColor = MaterialTheme.colorScheme.secondaryContainer

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
                            TextIcon(
                                text = stringResource(Res.string.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_ICON_TEXT),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_TITLE),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        gapSpacer()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIcon(
                                text = stringResource(Res.string.ONBOARDING_SECTION_LOAD_DATA_ICON_TEXT),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.ONBOARDING_SECTION_LOAD_DATA_TITLE),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(Res.string.ONBOARDING_SECTION_LOAD_DATA_SUBTITLE),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        gapSpacer()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextIcon(
                                text = stringResource(Res.string.ONBOARDING_SECTION_SHOW_DATA_ICON_TEXT),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.ONBOARDING_SECTION_SHOW_DATA_TITLE),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(Res.string.ONBOARDING_SECTION_SHOW_DATA_SUBTITLE),
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
