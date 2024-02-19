package ui.views

import Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.buttons.BackNavigationButton
import ui.composables.buttons.LoadDataButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDataView(
    loadData: () -> Unit,
    navigateUp: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Resources.HEADING_LABEL_LOAD_DATA, // "Daten Laden",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        BackNavigationButton(navigateUp)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LoadDataButton(loadData)
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier.padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Zur Abfrage Ihrer Daten werden Sie zu ID Austria weitergeleitet.")
            }
        }
    }
}
