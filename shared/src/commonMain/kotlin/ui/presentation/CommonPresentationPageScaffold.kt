package ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@ExperimentalMaterial3Api
@Composable
fun CommonPresentationPageScaffold(
    title: String,
    onNavigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUpIsClose: Boolean = false,
    content: @Composable (() -> Unit),
) {
    Scaffold(
        topBar = {
            CommonNavigateBackTopAppBar(
                title = title,
                onNavigateUp = onNavigateUp,
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings,
                navigateUpIsClose = navigateUpIsClose,
            )
        },
    ) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
            content()
        }
    }
}
