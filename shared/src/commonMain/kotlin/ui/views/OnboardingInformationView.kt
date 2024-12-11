package ui.views

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
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.heading_label_information_screen
import compose_wallet_app.shared.generated.resources.info_text_enthusiastic_welcome_end
import compose_wallet_app.shared.generated.resources.onboarding_section_load_data_icon_text
import compose_wallet_app.shared.generated.resources.onboarding_section_load_data_subtitle
import compose_wallet_app.shared.generated.resources.onboarding_section_load_data_title
import compose_wallet_app.shared.generated.resources.onboarding_section_show_data_icon_text
import compose_wallet_app.shared.generated.resources.onboarding_section_show_data_subtitle
import compose_wallet_app.shared.generated.resources.onboarding_section_show_data_title
import compose_wallet_app.shared.generated.resources.onboarding_section_terms_and_data_protection_icon_text
import compose_wallet_app.shared.generated.resources.onboarding_section_terms_and_data_protection_title
import org.jetbrains.compose.resources.stringResource
import ui.composables.TextIcon
import ui.composables.buttons.ContinueButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInformationView(
    onClickContinue: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.heading_label_information_screen),
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
                            text = stringResource(Res.string.info_text_enthusiastic_welcome_end),
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
                                text = stringResource(Res.string.onboarding_section_terms_and_data_protection_icon_text),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.onboarding_section_terms_and_data_protection_title),
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
                                text = stringResource(Res.string.onboarding_section_load_data_icon_text),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.onboarding_section_load_data_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(Res.string.onboarding_section_load_data_subtitle),
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
                                text = stringResource(Res.string.onboarding_section_show_data_icon_text),
                                color = textIconColor,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(Res.string.onboarding_section_show_data_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(Res.string.onboarding_section_show_data_subtitle),
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
