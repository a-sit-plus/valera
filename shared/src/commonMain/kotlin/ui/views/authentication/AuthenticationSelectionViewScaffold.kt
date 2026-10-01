package ui.views.authentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_continue
import at.asitplus.valera.resources.prompt_select_credential
import org.jetbrains.compose.resources.stringResource
import ui.composables.Logo
import ui.composables.buttons.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationSelectionViewScaffold(
    title: String,
    onClickLogo: () -> Unit,
    onNavigateUp: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton(onClick = onNavigateUp) },
            )
        },
        bottomBar = {
            Surface(color = NavigationBarDefaults.containerColor) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Text(stringResource(Res.string.prompt_select_credential), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onNext, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(Res.string.button_label_continue))
                    }
                }
            }
        },
        modifier = modifier,
    ) { Box(Modifier.padding(it)) { content() } }
}
