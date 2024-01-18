package ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = "Aktionen",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = resetApp,
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
