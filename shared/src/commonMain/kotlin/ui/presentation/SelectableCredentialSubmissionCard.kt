package ui.presentation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import ui.models.CredentialFreshnessValidationStateUiModel

interface SelectableCredentialSubmissionCard {
    @Composable
    operator fun invoke(
        isSelected: Boolean,
        allowMultiSelection: Boolean,
        onToggleSelection: (() -> Unit)?,
    )

    val credentialFreshnessSummary: StateFlow<CredentialFreshnessValidationStateUiModel>

    val matchingException: Throwable?
}