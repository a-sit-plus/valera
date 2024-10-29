package ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedCard
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
import compose_wallet_app.shared.generated.resources.description_read_terms
import compose_wallet_app.shared.generated.resources.heading_label_data_protection
import compose_wallet_app.shared.generated.resources.heading_label_navigate_back
import compose_wallet_app.shared.generated.resources.heading_label_terms_of_use
import compose_wallet_app.shared.generated.resources.heading_label_terms_of_use_and_data_protection
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.AcceptButton
import ui.composables.buttons.DetailsButton
import ui.composables.buttons.NavigateUpButton


@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun OnboardingTermsView(
    onClickNavigateBack: () -> Unit,
    onClickReadGeneralTermsAndConditions: () -> Unit,
    onClickReadDataProtectionPolicy: () -> Unit,
    onClickAccept: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    NavigateUpButton(onClick = onClickNavigateBack)
                },
                title = {
                    Text(
                        text = stringResource(Res.string.heading_label_navigate_back),
                        style = MaterialTheme.typography.titleLarge,
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
                    AcceptButton(
                        onClick = onClickAccept
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                val gapSpacer: @Composable () -> Unit = {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                Text(
                    text = stringResource(Res.string.heading_label_terms_of_use_and_data_protection),
                    style = MaterialTheme.typography.headlineLarge,
                )
                gapSpacer()
                Text(
                    text = stringResource(Res.string.description_read_terms),
                    style = MaterialTheme.typography.bodyMedium,
                )
                gapSpacer()
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.heading_label_terms_of_use),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DetailsButton(
                            onClick = onClickReadGeneralTermsAndConditions,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                gapSpacer()
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.heading_label_data_protection),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DetailsButton(
                            onClick = onClickReadDataProtectionPolicy,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
