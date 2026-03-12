package ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_credential_selection
import at.asitplus.valera.resources.info_text_available_credentials
import at.asitplus.valera.resources.info_text_no_credentials_available
import at.asitplus.valera.resources.info_text_requested_credentials
import org.jetbrains.compose.resources.stringResource
import ui.composables.ScreenHeading
import ui.theme.LocalExtendedColors

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelectionPageContent(
    selectableCredentialSubmissionCards: List<Pair<Boolean, SelectableCredentialSubmissionCard>>?,
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
                useBackButton = true,
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(it).fillMaxSize().verticalScroll(state = rememberScrollState()).padding(16.dp),
        ) {
            ScreenHeading(stringResource(Res.string.heading_label_credential_selection))

            Text(stringResource(Res.string.info_text_requested_credentials))
            CredentialSetQueryOptionSelectionCard(
                credentialRepresentationLocalized = credentialQueryUiModel.credentialRepresentationLocalized,
                credentialSchemeLocalized = credentialQueryUiModel.credentialSchemeLocalized,
                credentialAttributesLocalized = credentialQueryUiModel.requestedAttributesLocalized?.let {
                    it.attributesLocalized to it.otherAttributes
                },
                colors = if(selectableCredentialSubmissionCards.isNullOrEmpty()) {
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    val extendedColors = LocalExtendedColors.current
                    CardDefaults.elevatedCardColors(
                        containerColor = extendedColors.successContainer,
                        contentColor = extendedColors.onSuccessContainer
                    )
                }
            )
            if (!selectableCredentialSubmissionCards.isNullOrEmpty()) {
                Text(stringResource(Res.string.info_text_available_credentials))
                selectableCredentialSubmissionCards.withIndex().sortedBy {
                    !it.value.first
                }.forEach { (index, value) ->
                    val (isSelectable, credentialCard) = value
                    credentialCard(
                        isSelected = isCredentialOptionAtIndexSelected(index.toUInt()),
                        allowMultiSelection = allowMultiSelection,
                        onToggleSelection = if(isSelectable) {
                            {
                                onToggleCredentialOptionSelectedAtIndex(index.toUInt())
                            }
                        } else {
                            null
                        }
                    )
                }
            } else {
                Text(stringResource(Res.string.info_text_no_credentials_available))
            }
        }
    }
}



