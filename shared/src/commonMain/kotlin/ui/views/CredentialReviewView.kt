package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_keep_anyway
import at.asitplus.valera.resources.heading_label_credential_review
import at.asitplus.valera.resources.info_text_credential_review
import at.asitplus.valera.resources.prompt_credential_review
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.app.common.validation.PendingCredentialReview
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.validation.ValidationBanner
import ui.composables.validation.ValidationDetails
import ui.presentation.CommonBottomButtonsAbortContinue

/**
 * Issued credentials that did not pass every check, e.g. because their issuer is not trusted, with the outcome of
 * their validation, for the user to keep them anyway or to discard them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialReviewView(
    entries: List<PendingCredentialReview.Entry>,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    onClickLogo: () -> Unit,
    isBusy: Boolean = false,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.heading_label_credential_review),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Spacer(Modifier.width(15.dp))
                },
            )
        },
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_credential_review),
                onAbort = { if (!isBusy) onDiscard() },
                onContinue = { if (!isBusy) onKeep() },
                continueButtonLabel = Res.string.button_label_keep_anyway,
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.info_text_credential_review))
            entries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = entry.scheme.uiLabel(), style = MaterialTheme.typography.titleMedium)
                    ValidationBanner(entry.outcome)
                    ValidationDetails(entry.outcome)
                }
            }
        }
    }
}
