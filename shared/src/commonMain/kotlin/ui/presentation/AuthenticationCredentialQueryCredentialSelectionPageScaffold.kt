package ui.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@ExperimentalMaterial3Api
@Composable
fun AuthenticationCredentialQueryCredentialSelectionPageScaffold(
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onAbort: () -> Unit = onNavigateUp,
    content: @Composable (() -> Unit),
) {
    CommonAbortBackContinueScaffold(
        onContinue = onContinue,
        onAbort = onAbort,
        onClickLogo = onClickLogo,
        onNavigateUp = onNavigateUp,
        onClickSettings = onClickSettings,
        content = content,
    )
}

