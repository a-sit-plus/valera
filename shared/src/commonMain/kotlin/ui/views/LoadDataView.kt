package ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.composables.LabeledCheckbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDataView(
    navigateUp: () -> Unit,
    newDataItems: List<Pair<String, List<String>>> = listOf(),
    renewDataItems: List<Pair<String, List<String>>> = listOf(),
    loadData: (newDataItemIndices: List<Int>, renewDataItemIndices: List<Int>) -> Unit, // the indices of the items
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daten Laden",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                }
            )
        }
    ) {
        val dataSelectionRowModifier = Modifier.fillMaxWidth().padding(
            top = 8.dp,
            end = 24.dp,
            bottom = 8.dp,
            start = 16.dp,
        )

        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Welche Daten möchten Sie in diese App laden?")
                Spacer(modifier = Modifier.height(32.dp))
                if (newDataItems.isNotEmpty()) {
                    Column {
                        if(renewDataItems.isNotEmpty()) {
                            Text(
                                text = "Neue Daten laden",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Column {
                            Row(
                                modifier = dataSelectionRowModifier
                            ) {
                                LabeledCheckbox(
                                    label = if(renewDataItems.isNotEmpty()) "Alle neuen Daten" else "Alle Daten",
                                    checked = false,
                                    onCheckedChange = {

                                    },
                                )
                            }
                            for (itemIndex in newDataItems.indices) {
                                val item = newDataItems[itemIndex]
                                Row(
                                    modifier = dataSelectionRowModifier,
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = false,
                                            onCheckedChange = {},
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = item.first,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    IconButton(
                                        onClick = {}, // TODO: expand section
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "Zugehörige Attribute anzeigen"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



//@Composable
//fun DataSelectionRow(
//    label: String,
//    checked: Boolean,
//    onCheckedChange: (Boolean) -> Unit,
//    isDetailsExpanded: Boolean = false,
//    toggleDetailVisibility: (() -> Unit)? = null, // expand button won't be shown if there is no toggle
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth().padding(
//            top = 8.dp,
//            end = 24.dp,
//            bottom = 8.dp,
//            start = 16.dp
//        ),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween,
//    ) {
//        LabeledCheckbox(
//            label = label,
//            checked = checked,
//            onCheckedChange = onCheckedChange,
//            gapBetweenCheckboxAndLabel = 16.dp,
//            labelTextStyle = MaterialTheme.typography.bodyLarge,
//        )
//        if (toggleDetailVisibility != null) {
//            ExpandButton(
//                isExpanded = isDetailsExpanded,
//                toggleExpansion = toggleDetailVisibility,
//                contentDescription = if(isDetailsExpanded) {
//                    "Zugehörige Attribute verbergen"
//                } else {
//                    "Zugehörige Attribute anzeigen"
//                }
//            )
//        }
//    }
//}
