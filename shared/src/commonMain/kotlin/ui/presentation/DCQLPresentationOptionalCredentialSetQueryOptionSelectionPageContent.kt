package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import at.asitplus.data.NonEmptyList

@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationOptionalCredentialSetQueryOptionSelectionPageContent(
    credentialSetQueryOptionUiModels: NonEmptyList<CredentialSetQueryOptionUiModel>,
    onAbort: () -> Unit,
    onContinue: (() -> Unit)?,
    selectedOptionIndex: UInt?,
    isAnySelected: Boolean,
    onSetSelectedOptionIndex: (UInt?) -> Unit
) {
    DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
        isCredentialSetQueryRequired = false,
        selectedOptionIndex = selectedOptionIndex?.plus(1u) ?: 0u.takeIf {
            isAnySelected
        },
        credentialSetQueryOptionUiModels = credentialSetQueryOptionUiModels,
        onSelectCredentialSetQueryOptionAtIndex = {
            onSetSelectedOptionIndex(it.takeIf {
                it > 0u
            }?.minus(1u))
        },
        onAbort = onAbort,
        onContinue = onContinue.takeIf {
            isAnySelected
        }
    )
}