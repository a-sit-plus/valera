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
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_navigate_back
import org.jetbrains.compose.resources.stringResource
import ui.composables.buttons.NavigateUpButton

@Composable
@ExperimentalMaterial3Api
fun CommonNavigateBackTopAppBar(
    onNavigateUp: () -> Unit,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(Modifier.Companion.fillMaxWidth(), verticalAlignment = Alignment.Companion.CenterVertically) {
                Text(
                    stringResource(Res.string.heading_label_navigate_back),
                    modifier = Modifier.Companion.weight(1f),
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
            NavigateUpButton(onNavigateUp)
        },
    )
}
