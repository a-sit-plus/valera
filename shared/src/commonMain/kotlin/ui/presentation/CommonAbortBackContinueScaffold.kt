package ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.prompt_send_above_data
import org.jetbrains.compose.resources.stringResource


@ExperimentalMaterial3Api
@Composable
fun CommonAbortBackContinueScaffold(
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    onAbort: () -> Unit = onNavigateUp,
    content: @Composable (() -> Unit),
) {
    Scaffold(
        topBar = {
            CommonNavigateBackTopAppBar(
                onNavigateUp = onNavigateUp,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
            )
        },
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_send_above_data),
                onAbort = onAbort,
                onContinue = onContinue,
            )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            content()
        }
    }
}