package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import ui.presentation.CredentialSetQueryOptionSelectionCardCredentialQueryContent

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelectionPageContent(
    selectableCredentialSubmissionCards: List<SelectableCredentialSubmissionCard>?,
    allowMultiSelection: Boolean,
    onToggleCredentialOptionSelectedAtIndex: (UInt) -> Unit,
    isCredentialOptionAtIndexSelected: (UInt) -> Boolean,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    credentialQueryUiModel: DCQLCredentialQueryUiModel,
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

            if(!selectableCredentialSubmissionCards.isNullOrEmpty()) {
                selectableCredentialSubmissionCards.forEachIndexed { index, credentialCard ->
                    credentialCard(
                        isSelected = isCredentialOptionAtIndexSelected(index.toUInt()),
                        allowMultiSelection = allowMultiSelection,
                    ) {
                        onToggleCredentialOptionSelectedAtIndex(index.toUInt())
                    }
                }
            } else {
                // No credentials available, show the query that didn't match against anything
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CredentialSetQueryOptionSelectionCardCredentialQueryContent(
                            credentialRepresentationLocalized = credentialQueryUiModel.credentialRepresentationLocalized,
                            credentialSchemeLocalized = credentialQueryUiModel.credentialSchemeLocalized,
                            credentialAttributesLocalized = credentialQueryUiModel.requestedAttributesLocalized?.let {
                                it.attributesLocalized to it.otherAttributes
                            },
                        )
                    }
                }
                Text(stringResource(Res.string.info_text_no_credentials_available))
            }
        }
    }
}



