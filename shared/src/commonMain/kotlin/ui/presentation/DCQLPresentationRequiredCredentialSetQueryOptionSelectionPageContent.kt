package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@ExperimentalMaterial3Api
@Composable
fun DCQLPresentationRequiredCredentialSetQueryOptionSelectionPageContent(
    credentialSetQueryOptionUiModels: List<CredentialSetQueryOptionUiModel>,
    onAbort: () -> Unit,
    onContinue: (UInt) -> Unit,
) {
    var selectedOptionIndex by rememberSaveable {
        mutableStateOf(null as UInt?)
    }

    DCQLPresentationCredentialSetQueryOptionSelectionPageContent(
        isCredentialSetQueryRequired = true,
        selectedOptionIndex = selectedOptionIndex,
        credentialSetQueryOptionUiModels = credentialSetQueryOptionUiModels,
        onSelectCredentialSetQueryOptionAtIndex = {
            selectedOptionIndex = it
        },
        onAbort = onAbort,
        onContinue = {
            selectedOptionIndex?.let {
                onContinue(it)
            }
        }
    )
}