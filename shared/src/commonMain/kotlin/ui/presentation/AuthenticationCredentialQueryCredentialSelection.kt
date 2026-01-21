package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import at.asitplus.openid.dcql.DCQLCredentialQuery

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelection(
    credentialQuery: DCQLCredentialQuery,
    selectableCredentialSubmissionCards: List<SelectableCredentialSubmissionCard>?,
    onAbort: () -> Unit,
    onContinue: (List<UInt>) -> Unit,
) {
    val allowMultiSelection = credentialQuery.multiple ?: false

    val selectedIndices = rememberSaveable {
        mutableStateListOf<UInt>()
    }

    AuthenticationCredentialQueryCredentialSelectionPageContent(
        selectableCredentialSubmissionCards = selectableCredentialSubmissionCards,
        allowMultiSelection = allowMultiSelection,
        onToggleCredentialOptionSelectedAtIndex = {
            if(allowMultiSelection) {
                if(it in selectedIndices) {
                    selectedIndices.remove(it)
                } else {
                    selectedIndices.add(it)
                }
            } else {
                selectedIndices.clear()
                selectedIndices.add(it)
            }
        },
        isCredentialOptionAtIndexSelected = {
            it in selectedIndices
        },
        onAbort = onAbort,
        onContinue = if(selectedIndices.isNotEmpty()) {
            {
                onContinue(selectedIndices)
            }
        } else {
            null
        }
    )
}