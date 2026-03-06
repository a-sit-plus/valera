package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import at.asitplus.openid.dcql.DCQLCredentialQuery

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelection(
    credentialQueryUiModel: DCQLCredentialQueryUiModel,
    allowMultiSelection: Boolean,
    selectableCredentialSubmissionCards: List<SelectableCredentialSubmissionCard>?,
    onAbort: () -> Unit,
    onContinue: (Set<UInt>) -> Unit,
) {
    var selectedIndices by rememberSaveable(
        credentialQueryUiModel,
        stateSaver = listSaver(
            save = {
                it.toList()
            },
            restore = {
                it.toSet()
            }
        )
    ) {
        mutableStateOf<Set<UInt>>(setOf())
    }

    AuthenticationCredentialQueryCredentialSelectionPageContent(
        credentialQueryUiModel = credentialQueryUiModel,
        selectableCredentialSubmissionCards = selectableCredentialSubmissionCards,
        allowMultiSelection = allowMultiSelection,
        onToggleCredentialOptionSelectedAtIndex = {
            if (allowMultiSelection) {
                if (it in selectedIndices) {
                    selectedIndices = selectedIndices - it
                } else {
                    selectedIndices = selectedIndices + it
                }
            } else {
                selectedIndices = setOf(it)
            }
        },
        isCredentialOptionAtIndexSelected = {
            it in selectedIndices
        },
        onAbort = onAbort,
        onContinue = if (selectedIndices.also {
                println("selected indices: $selectedIndices")
            }.isNotEmpty()) {
            {
                onContinue(selectedIndices)
            }
        } else {
            null
        }
    )
}