package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.Page

class InformationPage : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationView(
    stage: String,
    version: String,
    resetApp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Informationen",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Stage: $stage")
                Text("Version: $version")
            }
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val layoutSpacingModifier = Modifier.padding(top = 24.dp)
                Column(
//                    modifier = layoutSpacingModifier // not for the first element
                ) {
                    val listSpacingModifier = Modifier.padding(top = 8.dp)
                    Column(
//                        modifier = listSpacingModifier, // not for the first element
                    ) {
                        val listItemSpacingModifier = Modifier.padding(top = 4.dp)
                        Text(
                            text = "Aktionen",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(
                            onClick = resetApp,
                            modifier = listItemSpacingModifier,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBackupRestore,
                                    contentDescription = "App zurücksetzen",
                                    modifier = Modifier.alignByBaseline().padding(end = 4.dp),
                                )
                                Text(
                                    "App zurücksetzen",
                                    modifier = Modifier.alignByBaseline(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListIconButton() {

}
