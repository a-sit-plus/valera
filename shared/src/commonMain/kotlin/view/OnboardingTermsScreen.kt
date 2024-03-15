package view

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composewalletapp.shared.generated.resources.DESCRIPTION_READ_TERMS
import composewalletapp.shared.generated.resources.HEADING_LABEL_DATA_PROTECTION
import composewalletapp.shared.generated.resources.HEADING_LABEL_NAVIGATE_BACK
import composewalletapp.shared.generated.resources.HEADING_LABEL_TERMS_OF_USE
import composewalletapp.shared.generated.resources.HEADING_LABEL_TERMS_OF_USE_AND_DATA_PROTECTION
import composewalletapp.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.AcceptButton
import ui.composables.buttons.DetailsButton
import ui.composables.buttons.NavigateUpButton


@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun OnboardingTermsScreen(
    onClickNavigateBack: () -> Unit,
    onClickReadGeneralTermsAndConditions: () -> Unit,
    onClickReadDataProtectionPolicy: () -> Unit,
    onClickAccept: () -> Unit,
) {
    val detailButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    NavigateUpButton(onClick = onClickNavigateBack)
                },
                title = {
                    Text(
                        text = stringResource(Res.string.HEADING_LABEL_NAVIGATE_BACK),
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
                    text = stringResource(Res.string.HEADING_LABEL_TERMS_OF_USE_AND_DATA_PROTECTION),
                    style = MaterialTheme.typography.headlineLarge,
                )
                gapSpacer()
                Text(
                    text = stringResource(Res.string.DESCRIPTION_READ_TERMS),
                    style = MaterialTheme.typography.bodyMedium,
                )
                gapSpacer()
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.HEADING_LABEL_TERMS_OF_USE),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DetailsButton(
                            onClick = onClickReadGeneralTermsAndConditions,
                            colors = detailButtonColors,
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
                        text = stringResource(Res.string.HEADING_LABEL_DATA_PROTECTION),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DetailsButton(
                            onClick = onClickReadDataProtectionPolicy,
                            colors = detailButtonColors,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
