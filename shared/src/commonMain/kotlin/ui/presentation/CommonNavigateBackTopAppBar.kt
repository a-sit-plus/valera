package ui.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.composables.buttons.NavigateUpButton

@Composable
@ExperimentalMaterial3Api
fun CommonNavigateBackTopAppBar(
    title: String,
    onNavigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    navigateUpIsClose: Boolean = false,
) {
    TopAppBar(
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        actions = {
            CommonNavigationActions(
                onClickLogo = onClickLogo,
                onClickSettings = onClickSettings
            )
        },
        navigationIcon = {
            NavigateUpButton(onNavigateUp, isClose = navigateUpIsClose)
        },
    )
}
