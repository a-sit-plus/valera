package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_credential_selection
import at.asitplus.valera.resources.info_text_no_credentials_available
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelectionPageContent(
    selectableCredentialSubmissionCards: List<SelectableCredentialSubmissionCard>?,
    allowMultiSelection: Boolean,
    onToggleCredentialOptionSelectedAtIndex: (UInt) -> Unit,
    isCredentialOptionAtIndexSelected: (UInt) -> Boolean,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
) {
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = null,
                onAbort = onAbort,
                onContinue = onContinue,
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(it).fillMaxSize().verticalScroll(state = rememberScrollState()).padding(16.dp),
        ) {
            ScreenHeading(stringResource(Res.string.heading_label_credential_selection))

            // TODO: Also show credential query details so user knows for which query this is selected?

            selectableCredentialSubmissionCards?.forEachIndexed { index, credentialCard ->
                credentialCard(
                    isSelected = isCredentialOptionAtIndexSelected(index.toUInt()),
                    allowMultiSelection = allowMultiSelection,
                ) {
                    onToggleCredentialOptionSelectedAtIndex(index.toUInt())
                }
            } ?: Text(stringResource(Res.string.info_text_no_credentials_available))
        }
    }
}



