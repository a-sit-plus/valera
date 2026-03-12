package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import at.asitplus.data.NonEmptyList

@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationRequiredCredentialSetQueryOptionSelectionPageContent(
    credentialSetQueryOptionUiModels: NonEmptyList<CredentialSetQueryOptionUiModel>,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    selectedOptionIndex: UInt?,
    onSetSelectedOptionIndex: (UInt) -> Unit
) {
    DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
        isCredentialSetQueryRequired = true,
        selectedOptionIndex = selectedOptionIndex,
        credentialSetQueryOptionUiModels = credentialSetQueryOptionUiModels,
        onSelectCredentialSetQueryOptionAtIndex = {
            onSetSelectedOptionIndex(it)
        },
        onAbort = onAbort,
        onContinue = onContinue
    )
}