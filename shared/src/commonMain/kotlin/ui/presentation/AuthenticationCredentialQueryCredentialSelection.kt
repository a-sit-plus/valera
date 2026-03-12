package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelection(
    credentialQueryUiModel: DCQLCredentialQueryUiModel,
    allowMultiSelection: Boolean,
    selectableCredentialSubmissionCards: List<Pair<Boolean, SelectableCredentialSubmissionCard>>,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    selectedIndices: Set<UInt>,
    onSelectIndices: (Set<UInt>) -> Unit,
) {
    AuthenticationCredentialQueryCredentialSelectionPageContent(
        credentialQueryUiModel = credentialQueryUiModel,
        selectableCredentialSubmissionCards = selectableCredentialSubmissionCards,
        allowMultiSelection = allowMultiSelection,
        onToggleCredentialOptionSelectedAtIndex = {
            if (allowMultiSelection) {
                if (it in selectedIndices) {
                    onSelectIndices(selectedIndices - it)
                } else {
                    onSelectIndices(selectedIndices + it)
                }
            } else {
                onSelectIndices(setOf(it))
            }
        },
        isCredentialOptionAtIndexSelected = {
            it in selectedIndices
        },
        onAbort = onAbort,
        onContinue = onContinue
    )
}