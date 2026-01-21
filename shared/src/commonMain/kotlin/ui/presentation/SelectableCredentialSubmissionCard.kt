package ui.presentation

import androidx.compose.runtime.Composable

fun interface SelectableCredentialSubmissionCard {
    @Composable
    operator fun invoke(
        isSelected: Boolean,
        allowMultiSelection: Boolean,
        onToggleSelection: (() -> Unit)?,
    )
}